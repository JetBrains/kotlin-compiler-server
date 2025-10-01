package com.compiler.server

import com.compiler.server.base.BaseExecutorTest
import com.compiler.server.lsp.utils.KotlinLspComposeExtension
import com.compiler.server.model.Project
import com.compiler.server.model.ProjectFile
import com.compiler.server.service.lsp.KotlinLspProxy
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest

class CompletionTest : BaseExecutorTest(), AbstractCompletionTest {
  override fun performCompletion(code: String, line: Int, character: Int, completions: List<String>, isJs: Boolean) {
      complete(code, line, character, completions, isJs)
  }
}

@ExtendWith(KotlinLspComposeExtension::class)
class LspCompletionTest : BaseExecutorTest(), AbstractCompletionTest {

    override fun performCompletion(
        code: String,
        line: Int,
        character: Int,
        completions: List<String>,
        isJs: Boolean,
    ) {
        runBlocking {
            val project = Project(files = listOf(ProjectFile(text = code, name = "test.kt")))
            lspProxy.getOneTimeCompletions(
                project = project,
                line = line,
                ch = character,
            )
        }
    }

    companion object {
        private val lspProxy = KotlinLspProxy()

        @BeforeAll
        @JvmStatic
        fun setUpLsp() = runBlocking {
            lspProxy.initializeClient()
        }
    }
}