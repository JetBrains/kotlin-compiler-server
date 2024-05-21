package com.compiler.server

import com.compiler.server.base.BaseExecutorTest
import com.compiler.server.base.renderErrorDescriptors
import com.compiler.server.model.ProjectSeveriry
import com.compiler.server.model.ProjectType
import org.junit.jupiter.api.Test

class ResourceCompileTest : BaseExecutorTest(), BaseResourceCompileTest {
  override fun request(code: String, platform: ProjectType) = when (platform) {
    ProjectType.JAVA -> run(code, "")
    ProjectType.JS_IR, ProjectType.JS -> translateToJsIr(code)
    else -> throw IllegalArgumentException("Unknown type $platform")
  }

  @Test
  fun `compile test from resource folder`() {
    checkResourceExamples(listOf(testDirJVM, testDirJS)) { result, _ ->
      val errors = result.compilerDiagnostics.filter { it.severity == ProjectSeveriry.ERROR }

      errors.takeIf { it.isNotEmpty() }?.let {
        renderErrorDescriptors(it)
      }
    }
  }
}
