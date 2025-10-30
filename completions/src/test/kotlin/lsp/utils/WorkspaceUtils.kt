package lsp.utils

import java.nio.file.Files
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.absolutePathString
import kotlin.io.path.copyToRecursively
import kotlin.io.path.deleteRecursively
import kotlin.io.path.toPath

object WorkspaceUtils {
    private const val TEMPLATE_NAME = "root-template"
    private val workspacePath = this::class.java.getResource("/lsp/workspaces")?.toURI()?.toPath()
        ?: error("no workspaces folder found")

    private const val GENERATED_NAME_PREFIX = "lsp-workspace-root-"

    @OptIn(ExperimentalPathApi::class)
    fun withTempWorkspace(
        kotlinVersion: String,
        block: suspend (localWorkspacePath: String, remoteWorkspacePath: String) -> Unit
    ) {
        val template = workspacePath.resolve(TEMPLATE_NAME)
        require(Files.exists(template)) { "no template found" }

        val dstWorkspaceName = "$GENERATED_NAME_PREFIX$kotlinVersion"
        val (dstDir, alreadyExists) = workspacePath.resolve(dstWorkspaceName).let {
            it to it.toFile().mkdirs()
        }

        if (!alreadyExists) {
            template.copyToRecursively(target = dstDir, overwrite = false, followLinks = false)

            val buildFile = dstDir.resolve("build.gradle.kts").also {
                require(Files.exists(it)) { "no build file found in dst dir" }
            }.toFile()

            val kotlinVersionPlaceholder = "{{kotlin_version}}"
            val pluginsPlaceholder = "{{plugins}}"
            val dependenciesPlaceholder = "{{dependencies}}"

            val newContent: String = listOf(kotlinVersionPlaceholder, pluginsPlaceholder, dependenciesPlaceholder)
                .zip(listOf(setOf(kotlinVersion), emptySet(), emptySet()))
                .fold(buildFile.readText()) { acc, (placeholder, values) ->
                    acc.replace(placeholder, values.joinToString("\n    "))
                }
            buildFile.writeText(newContent)
        }

        val localPath = dstDir.absolutePathString()
        val remotePath = "/workspaces/$dstWorkspaceName"

        block(localPath, remotePath)

        dstDir.deleteRecursively()
    }
}