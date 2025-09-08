package lsp.utils

import org.jetbrains.kotlin.konan.file.createTempDir
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ExtensionContext.Namespace
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource
import org.testcontainers.containers.ComposeContainer
import java.io.File

internal class KotlinLspComposeExtension : BeforeAllCallback {
    private lateinit var container: ComposeContainer

    override fun beforeAll(context: ExtensionContext) {
        val store = context.root.getStore(Namespace.create("lsp", "compose", "kotlin"))
        store.getOrComputeIfAbsent("kotlinLspCompose", {
            val composeFile = KotlinLspComposeExtension::class.java
                .getResource("/lsp/compose.yaml")
                ?.file
                ?: error("Could not find docker compose file")

            container = ComposeContainer(File(composeFile))
                .withExposedService("kotlin-lsp", 9999)

            container.start()

            val mappedPort = container.getServicePort("kotlin-lsp", 9999)

            System.setProperty("LSP_HOST", "localhost")
            System.setProperty("LSP_PORT", mappedPort.toString())
            System.setProperty("LSP_REMOTE_WORKSPACE_ROOT", "/lsp-users-projects-root-test")

            val localWorkspaceRoot = createTempDir("lsp-users-projects-root-test").also { it.deleteOnExitRecursively() }
            System.setProperty("LSP_LOCAL_WORKSPACE_ROOT", localWorkspaceRoot.absolutePath)

            LspIntegrationTestUtils.waitForLspReady(
                host = System.getProperty("LSP_HOST"),
                port = System.getProperty("LSP_PORT")!!.toInt(),
                workspace = System.getProperty("LSP_REMOTE_WORKSPACE_ROOT")
            )
            CloseableResource { container.stop() }
        }, CloseableResource::class.java)
    }
}
