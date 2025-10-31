package lsp.utils

import kotlin.io.path.createTempDirectory
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ExtensionContext.Namespace
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource
import org.testcontainers.containers.ComposeContainer
import java.io.File
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.absolutePathString
import kotlin.io.path.deleteRecursively

internal class KotlinLspComposeExtension : BeforeAllCallback {
    private lateinit var container: ComposeContainer

    @OptIn(ExperimentalPathApi::class)
    override fun beforeAll(context: ExtensionContext) {
        val store = context.root.getStore(Namespace.create("lsp", "compose", "kotlin"))
        store.getOrComputeIfAbsent("kotlinLspCompose", {
            val composeFile = KotlinLspComposeExtension::class.java
                .getResource("/lsp-compose.yml")
                ?.file
                ?: error("Could not find docker compose file")

            container = ComposeContainer(File(composeFile))
                .withExposedService("kotlin-lsp", 9999)

            container.start()

            val mappedPort = container.getServicePort("kotlin-lsp", 9999)

            System.setProperty("LSP_HOST", "localhost")
            System.setProperty("LSP_PORT", mappedPort.toString())
            System.setProperty("LSP_REMOTE_WORKSPACE_ROOT", "/workspaces/lsp-users-projects-root")

            val localWorkspaceRoot = createTempDirectory("lsp-users-projects-root")
            System.setProperty("LSP_LOCAL_WORKSPACE_ROOT", localWorkspaceRoot.absolutePathString())

            LspIntegrationTestUtils.waitForLspReady(
                host = System.getProperty("LSP_HOST"),
                port = System.getProperty("LSP_PORT")!!.toInt(),
                workspace = System.getProperty("LSP_REMOTE_WORKSPACE_ROOT")
            )
            CloseableResource {
                container.stop()
                localWorkspaceRoot.deleteRecursively()
            }

        }, CloseableResource::class.java)
    }
}
