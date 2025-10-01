package com.compiler.server.service.lsp

import com.compiler.server.service.lsp.components.LspProject
import com.compiler.server.model.Project
import com.compiler.server.model.ProjectFile
import com.compiler.server.service.lsp.client.LspClient
import com.compiler.server.service.lsp.client.RetriableLspClient
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
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.seconds

@Component
class KotlinLspProxy {

    internal lateinit var lspClient: LspClient
    internal val lspProjects = ConcurrentHashMap<Project, LspProject>()

    private val available = AtomicBoolean(false)
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
     * @param project the project containing the file
     * @param line the line number
     * @param ch the character position
     * @return a list of [CompletionItem]s
     */
    suspend fun getOneTimeCompletions(project: Project, line: Int, ch: Int): List<CompletionItem> {
        ensureLspClientReady() ?: return emptyList()
        val lspProject = lspProjects.getOrPut(project) { createNewProject(project) }
        val projectFile = project.files.first()
        val uri = lspProject.getDocumentUri(projectFile.name) ?: return emptyList()
        lspClient.openDocument(uri, projectFile.text, 1)
        return getCompletions(lspProject, line, ch, projectFile.name)
            .also { closeProject(project) }
    }

    /**
     * Retrieve completions for a given line and character position in a project file. By now
     *
     * - We assume that the project contains a single file;
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
     * [workspacePath] is the path ([[java.net.URI.path]]) to the root project directory,
     * where the project must be a project supported by [Kotlin-LSP](https://github.com/Kotlin/kotlin-lsp).
     * The workspace will not contain users' files, but it can be used to store common files,
     * to specify kotlin/java versions, project-wide imported libraries and so on.
     *
     * @param workspacePath the path to the workspace directory, namely the root of the common project
     * @param clientName the name of the client, defaults to "lsp-proxy"
     */
    suspend fun initializeClient(
        workspacePath: String = LSP_REMOTE_WORKSPACE_ROOT.path,
        clientName: String = "kotlin-compiler-server"
    ) {
        if (!::lspClient.isInitialized) {
            lspClient = LspClient.createSingle(workspacePath, clientName)
            wireAvailabilityObservers(lspClient)
            available.set(true)
            lspClientInitializedDeferred.complete(Unit)
        }
    }

    fun isAvailable(): Boolean = available.get()

    suspend fun requireAvailable() {
        if (!isAvailable()) {
            try {
                if (!::lspClient.isInitialized) initializeClient()
                if (!lspClientInitializedDeferred.isCompleted) {
                    lspClientInitializedDeferred.await()
                }
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

    private fun createNewProject(project: Project): LspProject = LspProject.fromProject(project)

    internal fun closeProject(project: Project) {
        val lspProject = lspProjects[project] ?: return
        lspProject.getDocumentsUris().forEach { uri -> lspClient.closeDocument(uri) }
        lspProject.tearDown()
        lspProjects.remove(project)
    }

    fun closeAllProjects() {
        lspProjects.keys.forEach { closeProject(it) }
        lspProjects.clear()
    }

    fun wireAvailabilityObservers(client: LspClient) {
        (client as? RetriableLspClient)?.let { lspClient ->
            lspClient.addOnDisconnectListener {
                if (!lspClientInitializedDeferred.isCompleted) {
                    lspClientInitializedDeferred.completeExceptionally(IOException("Lsp client disconnected"))
                }
                lspClientInitializedDeferred = CompletableDeferred()
                available.set(false)
            }
            lspClient.addOnReconnectListener {
                lspProjects.forEach { (project, lspProject) ->
                    val file = project.files.first()
                    val uri = lspProject.getDocumentUri(file.name) ?: return@forEach
                    lspProject.resetDocumentVersion(file.name)
                    client.openDocument(uri, file.text, 1)
                }
                lspClientInitializedDeferred.complete(Unit)
                available.set(true)
            }
        }
    }

    /**
     * [LSP_REMOTE_WORKSPACE_ROOT] is the workspace that the LSP will point to, while
     * [LSP_LOCAL_WORKSPACE_ROOT] is the local workspace that the LSP client is running on.
     * They are usually the same if the LSP client is running on the same machine as the server,
     * otherwise [LSP_REMOTE_WORKSPACE_ROOT] will have to be set wrt to server's local workspace.
     *
     * Note that [LSP_REMOTE_WORKSPACE_ROOT] is the most important one, since it will be used
     * from the LSP analyzer to resolve the project's dependencies.
     */
    companion object {
        private val logger = LoggerFactory.getLogger(KotlinLspProxy::class.java)

        val LSP_REMOTE_WORKSPACE_ROOT: URI = Path.of(
            System.getProperty("LSP_REMOTE_WORKSPACE_ROOT")
                ?: System.getenv("LSP_REMOTE_WORKSPACE_ROOT")
                ?: defaultWorkspacePath()
        ).toUri()

        val LSP_LOCAL_WORKSPACE_ROOT: URI = Path.of(
            System.getProperty("LSP_LOCAL_WORKSPACE_ROOT")
                ?: System.getenv("LSP_LOCAL_WORKSPACE_ROOT")
                ?: defaultWorkspacePath()
        ).toUri()

        private fun defaultWorkspacePath(): String =
            System.getProperty("LSP_USERS_PROJECTS_ROOT")
                ?: System.getProperty("LSP_USERS_PROJECTS_ROOT")
                ?: ("lsp-users-projects-root")
    }
}

object StatefulKotlinLspProxy {
    private val clientsProjects = ConcurrentHashMap<String, Project>()

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
     * @param newProject the project containing the file
     * @param line the line number
     * @param ch the character position
     * @return a list of [CompletionItem]s
     */
    suspend fun KotlinLspProxy.getCompletionsForClient(
        clientId: String,
        newProject: Project,
        line: Int,
        ch: Int
    ): List<CompletionItem> {
        val project = clientsProjects[clientId] ?: return emptyList()
        val lspProject = lspProjects[project] ?: return emptyList()
        val newContent = newProject.files.first().text
        val documentToChange = project.files.first().name
        changeDocumentContent(lspProject, documentToChange, newContent)
        return getCompletions(lspProject, line, ch, documentToChange)
    }

    fun KotlinLspProxy.onClientConnected(clientId: String) {
        val project = Project(files = listOf(ProjectFile(name = "$clientId.kt")))
            .also { clientsProjects[clientId] = it }
        val lspProject = LspProject.fromProject(project).also { lspProjects[project] = it }
        lspProject.getDocumentsUris().forEach { uri -> lspClient.openDocument(uri, "", 1) }
    }

    fun KotlinLspProxy.onClientDisconnected(clientId: String) {
        clientsProjects[clientId]?.let {
            closeProject(it)
            clientsProjects.remove(clientId)
        }
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