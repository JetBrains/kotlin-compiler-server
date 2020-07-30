package com.compiler.server

import com.compiler.server.base.BaseExecutorTest
import com.compiler.server.model.ErrorDescriptor
import com.compiler.server.model.ProjectSeveriry
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class HighlightWithImportTest : BaseExecutorTest() {
    @Test
    fun `base highlight with imports ok`() {
        val highlights = highlightWithImport("\nfun main() {\n    println(\"Hello, world!!!\")\n}")
        Assertions.assertTrue(highlights.values.flatten().isEmpty())
    }

    @Test
    fun `highlight with imports Unresolved class reference`() {
        val highlights = highlightWithImport("fun main() {\n   val s = Random(5)\n}")
        errorContains(highlights,
            "Unresolved reference: Random. Suggestions for import: kotlin.random.Random")
    }

    @Test
    fun `highlight with import Unresolved function reference`() {
        val highlights = highlightWithImport("fun main() {\n   val s = sin(5.0)\n}")
        errorContains(highlights,
            "Unresolved reference: sin. Suggestions for import: kotlin.math.sin")
    }

    private fun errorContains(highlights: Map<String, List<ErrorDescriptor>>, message: String) {
        Assertions.assertTrue(highlights.values.flatten().map { it.message }.any { it.contains(message) })
        Assertions.assertTrue(highlights.values.flatten().map { it.severity }.any { it == ProjectSeveriry.ERROR })
    }
}