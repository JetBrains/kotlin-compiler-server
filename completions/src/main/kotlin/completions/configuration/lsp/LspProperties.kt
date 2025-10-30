package completions.configuration.lsp

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration properties for the Kotlin-LSP server client. These properties
 * are bound to the `lsp` prefix in the configuration file.
 *
 * > Note: the default behaviour of this component expects the Kotlin-LSP instancy running locally
 * on the same machine as this application is running. In this case, there's no difference between
 * [local][localWorkspaceRoot] and [remote][remoteWorkspaceRoot] workspaces. In this case, the paths
 * are computed as absolute paths.
 * However, if [local][localWorkspaceRoot] or [remote][remoteWorkspaceRoot] are overridden in the application
 * configuration file, they will not be resolved and are **used as is**.
 *
 * @property host The hostname of the LSP server.
 * @property port The port number to connect to the LSP server.
 * @property reconnectionRetries The maximum number of reconnection attempts in case of connection failure.
 * @property kotlinVersion The Kotlin version used by the LSP client. Defaults to the current runtime Kotlin version.
 * @property remoteWorkspaceRoot The root directory for the LSP workspace wrt to the LSP server, derived from the provided Kotlin version.
 * @property localWorkspaceRoot The root directory for the LSP workspace wrt to the host machine, derived from the provided Kotlin version.
 */
@ConfigurationProperties(prefix = "lsp")
data class LspProperties(
    val host: String,
    val port: Int,
    val reconnectionRetries: Int,
    val kotlinVersion: String = KotlinVersion.CURRENT.toString(),
    val remoteWorkspaceRoot: String = resolveWorkspacePaths(kotlinVersion),
    val localWorkspaceRoot: String = resolveWorkspacePaths(kotlinVersion),
) {

    fun forKotlinVersion(kotlinVersion: String): LspProperties =
        this.copy(
            kotlinVersion = kotlinVersion,
            remoteWorkspaceRoot = resolveWorkspacePath(this.remoteWorkspaceRoot, kotlinVersion),
            localWorkspaceRoot = resolveWorkspacePath(this.localWorkspaceRoot, kotlinVersion)
        )

    companion object {
        private const val DEFAULT_WORKSPACE_ROOT_PATH: String = "/lsp/workspaces/lsp-workspace-root-"

        private fun resolveWorkspacePath(workspacePath: String, kotlinVersion: String) = workspacePath + kotlinVersion

        private fun resolveWorkspacePaths(kotlinVersion: String): String {
            val path = resolveWorkspacePath(DEFAULT_WORKSPACE_ROOT_PATH, kotlinVersion)
            return this::class.java.getResource(path)?.toURI()?.path
                ?: error(
                    """
                    Could not find '$path' in project resources. Please make sure you either do one of the following:
                      - Generate workspace for kotlin=$kotlinVersion with `./gradlew :completions:generateLspWorkspaceRoot`
                      - Override in the `application.yml` `remoteWorkspaceRoot` and `localWorkspaceRoot`.
                    """.trimIndent()
                )
        }
    }
}