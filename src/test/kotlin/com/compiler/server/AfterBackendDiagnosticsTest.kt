package com.compiler.server

import com.compiler.server.base.BaseExecutorTest
import com.compiler.server.base.errorContains
import org.junit.jupiter.api.Test

class AfterBackendDiagnosticsTest : BaseExecutorTest() {
  @Test
  fun `declaration clash test`() {
    val diagnostics = highlight(code = """
      fun main() {}
      class A { constructor(x: Int); constructor(x: UInt) }
    """.trimIndent())
    errorContains(diagnostics, """
      Platform declaration clash: The following declarations have the same JVM signature (<init>(I)V):
          constructor A(x: Int) defined in A
          constructor A(x: UInt) defined in A
    """.trimIndent())
  }
}
