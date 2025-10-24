package completions.lsp

import completions.dto.api.CompletionRequest
import completions.dto.api.ProjectFile
import completions.exceptions.LspUnavailableException
import completions.lsp.client.LspClient
import completions.lsp.client.ReconnectingLspClient
import completions.lsp.components.LspProject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.eclipse.lsp4j.CompletionItem
import org.eclipse.lsp4j.Position
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.io.IOException
import java.net.URI
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.seconds

@Component
class KotlinLspProxy {

    internal lateinit var lspClient: LspClient
    internal val lspProjects = CopyOnWriteArrayList<LspProject>()

    private val available = AtomicBoolean(false)
    private val isInitializing = AtomicBoolean(false)
    private var lspClientInitializedDeferred = CompletableDeferred<Unit>()

    private val proxyCoroutineScope =
        CoroutineScope(Dispatchers.IO + CoroutineName("KotlinLspProxy"))

    @EventListener(ApplicationReadyEvent::class)
    fun initClientOnReady() {
        proxyCoroutineScope.launch {
            initializeClient()
        }
    }

    /**
     * Retrieve completions for a given line and character position in a project file.
     * The document will be opened, completion triggered and then closed.
     *
     * This modality is aimed for **stateless** scenarios where we don't care about
     * the identity of the client and the project.
     *
     * @param request the completion request
     * @param line the line number
     * @param ch the character position
     * @param projectFile the file to be used for completion, defaults to the first file in the project
     * @return a list of [CompletionItem]s
     */
    suspend fun getOneTimeCompletions(
        request: CompletionRequest,
        line: Int,
        ch: Int,
        projectFile: ProjectFile = request.files.first(),
    ): List<CompletionItem> {
        ensureLspClientReady() ?: return emptyList()
        val lspProject = createNewProject(request).also(lspProjects::add)
        val uri = lspProject.getDocumentUri(projectFile.name) ?: return emptyList()
        lspClient.openDocument(uri, projectFile.text, 1)
        return getCompletions(lspProject, line, ch, projectFile.name)
            .also { closeProject(lspProject) }
    }

    /**
     * Retrieve completions for a given line and character position in a project file. By now
     *
     * - We assume that the project contains a single file;
     *   - TODO(KTL-3757): support multiple files in the same project
     * - Changes arrive **before** completion is triggered.
     *
     * Changes are not incremental, whole file content is transmitted. Future support
     * for incremental changes may be added when [Kotlin-LSP](https://github.com/Kotlin/kotlin-lsp)
     * supports it.
     *
     * @param project the project containing the file
     * @param line the line number
     * @param ch the character position
     * @param fileName the name of the file to be used for completion
     * @return a list of [CompletionItem]s
     */
    internal suspend fun getCompletions(
        project: LspProject,
        line: Int,
        ch: Int,
        fileName: String,
    ): List<CompletionItem> {
        val uri = project.getDocumentUri(fileName) ?: return emptyList()
        return lspClient.getCompletionsWithRetry(uri, Position(line, ch))
    }

    /**
     * Initialize the LSP client. This method must be called before any other method in this
     * class. It is recommended to call this method when the **spring** application context is initialized.
     *
     * [workspacePath] is the path ([[URI.path]]) to the root project directory,
     * where the project must be a project supported by [Kotlin-LSP](https://github.com/Kotlin/kotlin-lsp).
     * The workspace will not contain users' files, but it can be used to store common files,
     * to specify kotlin/java versions, project-wide imported libraries and so on.
     *
     * @param workspacePath the path to the workspace directory, namely the root of the common project
     * @param clientName the name of the client, defaults to "lsp-proxy"
     */
    suspend fun initializeClient(
        workspacePath: String = lspRemoteWorkspaceRoot().path,
        clientName: String = "kotlin-compiler-server"
    ) {
        if (!::lspClient.isInitialized) {
            if (isInitializing.getAndSet(true)) return
            lspClient = LspClient.createSingle(workspacePath, clientName)
            wireAvailabilityObservers(lspClient)
            available.set(true)
            lspClientInitializedDeferred.complete(Unit)
        }
    }

    fun isAvailable(): Boolean = available.get()

    /**
     * Checks whether the LSP client is available. If not, it will be initialized.
     *
     * @throws completions.exceptions.LspUnavailableException if the client is not available after 60 seconds
     */
    suspend fun requireAvailable() {
        if (!isAvailable()) {
            try {
                if (!::lspClient.isInitialized) initializeClient()
                if (!lspClientInitializedDeferred.isCompleted) lspClientInitializedDeferred.await()
                lspClient.awaitReady(60.seconds)
                available.set(true)
            } catch (e: Exception) {
                throw LspUnavailableException(e.message ?: "Lsp client is not available")
            }
        }
    }

    internal suspend fun ensureLspClientReady(): Boolean? =
        runCatching { requireAvailable() }.onFailure {
            if (it is LspUnavailableException) throw it
            else logger.debug("Lsp client not ready: ${it.message}")
        }.isSuccess.takeIf { it }

    private fun createNewProject(project: CompletionRequest): LspProject = LspProject.fromCompletionRequest(project)

    internal fun closeProject(lspProject: LspProject) {
        lspProject.getDocumentsUris().forEach { uri -> lspClient.closeDocument(uri) }
        lspProject.tearDown()
        lspProjects.remove(lspProject)
    }

    fun closeAllProjects() {
        lspProjects.forEach { closeProject(it) }
        lspProjects.clear()
    }

    /**
     * Configures availability observers for the provided [LspClient].
     * If the client is of type `ReconnectingLspClient`, this method sets up listeners to handle
     * client disconnection and reconnection events, ensuring proper maintenance of the client's state.
     *
     * On disconnect, the method updates the availability status and resets the necessary internal states.
     * On reconnect, it reopens documents, resets their versions, and updates the availability status.
     *
     * @param client the LSP client to which the availability observers will be wired
     */
    fun wireAvailabilityObservers(client: LspClient) {
        (client as? ReconnectingLspClient)?.let { lspClient ->
            lspClient.addOnDisconnectListener {
                if (!lspClientInitializedDeferred.isCompleted) {
                    lspClientInitializedDeferred.completeExceptionally(IOException("Lsp client disconnected"))
                }
                lspClientInitializedDeferred = CompletableDeferred()
                available.set(false)
                isInitializing.set(false)
            }
            lspClient.addOnReconnectListener {
                lspProjects.forEach { project ->
                    val file = project.files.first()
                    val uri = project.getDocumentUri(file.name) ?: return@forEach
                    project.resetDocumentVersion(file.name)
                    client.openDocument(uri, file.text, 1)
                }
                lspClientInitializedDeferred.complete(Unit)
                available.set(true)
                isInitializing.set(false)
            }
        }
    }

    /**
     * [lspRemoteWorkspaceRoot] is the workspace that the LSP will point to, while
     * [lspLocalWorkspaceRoot] is the local workspace that the LSP client is running on.
     * They are usually the same if the LSP client is running on the same machine as the server,
     * otherwise [lspRemoteWorkspaceRoot] will have to be set wrt to server's local workspace.
     *
     * Note that [lspRemoteWorkspaceRoot] is the most important one, since it will be used
     * from the LSP analyzer to resolve the project's dependencies.
     */
    companion object {
        private val logger = LoggerFactory.getLogger(KotlinLspProxy::class.java)

        fun lspRemoteWorkspaceRoot(): URI =Path.of(
            System.getProperty("LSP_REMOTE_WORKSPACE_ROOT")
                ?: System.getenv("LSP_REMOTE_WORKSPACE_ROOT")
                ?: defaultWorkspacePath()
        ).toUri()

        fun lspLocalWorkspaceRoot(): URI = Path.of(
            System.getProperty("LSP_LOCAL_WORKSPACE_ROOT")
                ?: System.getenv("LSP_LOCAL_WORKSPACE_ROOT")
                ?: defaultWorkspacePath()
        ).toUri()

        private fun defaultWorkspacePath(): String =
            System.getProperty("LSP_USERS_PROJECTS_ROOT")
                ?: System.getProperty("LSP_USERS_PROJECTS_ROOT")
                ?: run {
                    KotlinLspProxy::class.java.getResource("/lsp-users-projects-root")?.path
                        ?: error("Could not find default workspace path")
                }
    }
}

/**
 * This extension object manages stateful operations for interacting with a Kotlin-based LSP client.
 * It provides functionality for handling client connections, managing associated project states, and performing
 * completion operations or modifying documents contents.
 *
 * Note: regarding KTL-3757, this extension object is ready to support multiple files in the same project,
 * but some mechanisms of resources releasing must be taken into account because currently we take care of
 * opening new documents, but not closing them when client closes/deletes them.
 * Custom WS messages could be designed to handle this.
 */
object StatefulKotlinLspProxy {
    private val clientsProjects = ConcurrentHashMap<String, LspProject>()

    /**
     * Retrieve completions for a given line and character position in a project file.
     * This modality is used for **stateful** scenarios, where the document will be
     * changed and then completion triggered, while it's being stored in memory
     * for the whole user session's duration.
     *
     * Please note that calling this method assumes that **the client** has **already opened
     * the document**.
     *
     * @param clientId the user identifier (or session identifier)
     * @param completionRequest the request containing the files
     * @param line the line number
     * @param ch the character position
     * @return a list of [CompletionItem]s
     */
    suspend fun KotlinLspProxy.getCompletionsForClient(
        clientId: String,
        completionRequest: CompletionRequest,
        line: Int,
        ch: Int,
        projectFile: ProjectFile = completionRequest.files.first(),
    ): List<CompletionItem> {
        val lspProject = clientsProjects[clientId]?.let {
            ensureDocumentPresent(it, projectFile, clientId)
        } ?: return emptyList()

        val newContent = projectFile.text
        val documentToChange = projectFile.name
        changeDocumentContent(lspProject, documentToChange, newContent)
        return getCompletions(lspProject, line, ch, documentToChange)
    }

    /**
     * This method initializes a new project for the connected client, sets up an LSP project
     * from the initialized project, and opens all relevant document URIs within the project.
     *
     * @param clientId the unique identifier for the client that has connected
     */
    fun KotlinLspProxy.onClientConnected(clientId: String) {
        val lspProject = LspProject.empty(clientId).also {
            lspProjects.add(it)
            clientsProjects[clientId] = it
        }
        lspProject.getDocumentsUris().forEach { uri -> lspClient.openDocument(uri, "", 1) }
    }

    /**
     * This method closes the LSP project for the connected client and removes the project from the proxy
     * and frees up its resources.
     *
     * @param clientId the unique identifier for the client that has disconnected
     */
    fun KotlinLspProxy.onClientDisconnected(clientId: String) {
        clientsProjects[clientId]?.let {
            closeProject(it)
            clientsProjects.remove(clientId)
            lspProjects.remove(it)
        }
    }

    /**
     * Ensure that the provided project contains the specified file; if the file is not present,
     * it will be added to the project and open the file in the LSP client.
     *
     * @param project the project to be modified if necessary
     * @param projectFile the file to be opened if not present in the project
     * @return the modified project, with the file added if necessary
     */
    private fun KotlinLspProxy.ensureDocumentPresent(
        project: LspProject,
        projectFile: ProjectFile,
        clientId: String
    ): LspProject {
        if (project.containsFile(projectFile)) return project
        lspProjects.remove(project)
        val newLspProject = project.copy(files = project.files + projectFile).also {
                lspProjects.add(it)
                clientsProjects[clientId] = it
            }
        newLspProject.getDocumentUri(projectFile.name)?.let { uri ->
            lspClient.openDocument(uri, projectFile.text, 1)
        }
        return newLspProject
    }

    private fun KotlinLspProxy.changeDocumentContent(
        lspProject: LspProject,
        documentToChange: String,
        newContent: String
    ) {
        lspProject.changeDocumentContents(documentToChange, newContent)
        lspClient.changeDocument(
            lspProject.getDocumentUri(documentToChange)!!,
            newContent,
            lspProject.getLatestDocumentVersion(documentToChange)
        )
    }
}