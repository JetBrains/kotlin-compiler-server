package completions.lsp.components

import completions.dto.api.CompletionRequest
import completions.dto.api.ProjectFile
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.createTempDirectory

/**
 * Represents a LSP project. This class manages documents, their versions,
 * and the project's filesystem structure. It facilitates tasks such as creating a project workspace,
 * managing documents within that workspace, and tearing down the workspace when it's no longer needed.
 *
 * @property files A list of [ProjectFile] objects representing project files.
 */
data class LspProject(val files: List<ProjectFile> = emptyList(), val ownerId: String? = null) {

    private val projectRoot: Path = baseDir.resolve("${ownerId?.let { "user-$it" } ?: UUID.randomUUID()}")
    private val documentsToPaths: MutableMap<String, Path> = mutableMapOf()
    private val documentsVersions = ConcurrentHashMap<String, AtomicInteger>()

    init {
        projectRoot.toFile().mkdirs()
        files.associateTo(documentsToPaths) { file ->
            file.name to projectRoot.resolve(file.name).also {
                it.toFile().writeText(file.text)
            }
        }
    }

    @Synchronized
    fun changeDocumentContents(name: String, newContents: String) {
        documentsToPaths[name]?.toFile()?.writeText(newContents)
        documentsVersions[name]?.incrementAndGet()
    }

    fun containsFile(file: ProjectFile): Boolean = files.any { it.name == file.name && it.text == file.text}

    /**
     * Returns the URI of a document in the project compliant with the [completions.lsp.client.LspClient].
     */
    fun getDocumentUri(name: String): String? = documentsToPaths[name]?.toUri()?.toString()

    fun getDocumentsUris(): List<String> = documentsToPaths.keys.mapNotNull { getDocumentUri(it) }

    fun getLatestDocumentVersion(name: String): Int = documentsVersions.compute(name) { _, v ->
        (v ?: AtomicInteger(1)).apply { addAndGet(1) }
    }!!.get()

    fun resetDocumentVersion(name: String) {
        documentsVersions[name]?.set(1)
    }

    /**
     * Tears down the project workspace and deletes all project files.
     */
    fun tearDown() {
        documentsToPaths.values.forEach { it.toFile().delete() }
        projectRoot.toFile().delete()
    }

    companion object {
        private val baseDir = createTempDirectory("lsp-users-projects")

        /**
         * Creates a new instance of [LspProject] based on the provided [CompletionRequest] data.
         * Please note that currently only JVM-related projects are supported.
         *
         * @param completionRequest the [CompletionRequest] data to use for project creation
         * @param ownerId optional identifier for the owner of the project
         * @return a new [LspProject] instance with the provided project's configuration type and files
         */
        fun fromCompletionRequest(completionRequest: CompletionRequest, ownerId: String? = null): LspProject {
            return LspProject(
                files = completionRequest.files.map { ProjectFile(it.text, it.name) },
                ownerId = ownerId,
            )
        }

        /**
         * Creates a new empty [LspProject] instance, with a single empty file.
         */
        fun empty(ownerId: String? = null): LspProject = LspProject(
            files = listOf(ProjectFile("", "File.kt")),
            ownerId = ownerId
        )
    }
}