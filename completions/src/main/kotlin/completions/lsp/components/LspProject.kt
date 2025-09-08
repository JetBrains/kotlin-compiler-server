package completions.lsp.components

import completions.lsp.KotlinLspProxy
import completions.model.Project
import completions.model.ProjectType
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Represents a LSP project. This class manages documents, their versions,
 * and the project's filesystem structure. It facilitates tasks such as creating a project workspace,
 * managing documents within that workspace, and tearing down the workspace when it's no longer needed.
 *
 * @property confType The configuration type of the project, defaulting to `ProjectType.JAVA`.
 * @property files A list of [LspDocument] objects representing project files.
 */
class LspProject(
    confType: ProjectType = ProjectType.JAVA,
    files: List<LspDocument> = emptyList(),
    ownerId: String? = null,
) {
    private val projectRoot: Path = baseDir.resolve("$confType-${ownerId ?: UUID.randomUUID().toString()}")
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
        private val baseDir = Path.of(KotlinLspProxy.lspLocalWorkspaceRoot()).toAbsolutePath()

        /**
         * Creates a new instance of [LspProject] based on the provided [Project] data.
         * Please note that currently only JVM-related projects are supported.
         *
         * @param project the source project containing configuration type and a list of files
         * @param ownerId optional identifier for the owner of the project
         * @return a new [LspProject] instance with the provided project's configuration type and files
         */
        fun fromProject(project: Project, ownerId: String? = null): LspProject {
            return LspProject(
                confType = ensureSupportedConfType(project.confType),
                files = project.files.map { LspDocument(it.text, it.name) },
                ownerId = ownerId,
            )
        }

        /**
         * If and when kotlin LSP support other project types, this function can be updated.
         */
        private fun ensureSupportedConfType(projectType: ProjectType): ProjectType {
            require(projectType == ProjectType.JAVA) { "Only JVM related projects are supported" }
            return projectType
        }
    }
}

data class LspDocument(
    val text: String = "",
    val name: String = "File.kt",
    val publicId: String? = null,
)

@Suppress("unused")
enum class LspProjectType(val id: String) {
    JAVA("java"),
    // add here support for JS, WASM, ...
}