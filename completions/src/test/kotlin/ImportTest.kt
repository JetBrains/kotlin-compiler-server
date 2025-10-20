import base.BaseCompletionTest
import completions.dto.api.Completion
import lsp.utils.CARET_MARKER
import org.junit.jupiter.api.Test
import kotlin.test.Ignore
import kotlin.test.assertFalse
import kotlin.test.assertTrue

interface ImportTest : BaseCompletionTest {
    @Test
    fun `import class`() {
        doImportClassTest()
    }

    @Test
    fun `import class with import`() {
        val code = ("""
            import kotlin.math.sin
            import java.util.Random
            fun main() {
                val rand = Random$CARET_MARKER
            }
        """.trimIndent())

        val foundCompletions = getCompletions(codeWithCaret = code)
        completionContainsCheckOtherImports(
            foundCompletions = foundCompletions,
            completions = listOf(
                Pair("Random (kotlin.random)", true)
            )
        )
    }

    @Test
    fun `import method with other import`() {
        doImportMethodWithOtherImportTest()
    }

    @Test
    fun `import class with parameters`() {
        doImportClassWithParametersTest()
    }

    @Test
    fun `import method`() {
        doImportMethodTest()
    }

    @Test
    fun `open bracket after import completion`() {
        doOpenBracketAfterImportCompletionTest()
    }

    // TODO(IJPL-213504) Auto-completion/auto-import issue with external library
    @Ignore
    @Test
    fun `brackets after import completion`() {
        val code = """
            fun main() {
                val timeZone  = getDefaultTimeZone$CARET_MARKER
            }
        """.trimIndent()

        val foundCompletionsTexts = getCompletions(codeWithCaret = code).map { it.text }
        val completions = listOf(
            "com.fasterxml.jackson.databind.util.StdDateFormat.getDefaultTimeZone()"
        )
        completions.forEach {
            assertTrue(
                foundCompletionsTexts.contains(it),
                "Wrong completion text for import. Expected to find $it in $foundCompletionsTexts"
            )
        }
    }

    @Test
    fun `import class js`() {
        doImportClassTest(isJs = true)
    }

    @Test
    fun `import method with other import js`() {
        doImportMethodWithOtherImportTest(isJs = true)
    }

    @Test
    fun `import class with parameters js`() {
        doImportClassWithParametersTest(isJs = true)
    }

    @Test
    fun `import method js`() {
        doImportMethodTest(isJs = true)
    }

    @Test
    fun `open bracket after import completion js`() {
        doOpenBracketAfterImportCompletionTest(isJs = true)
    }

    // TODO(IJPL-213504) Auto-completion/auto-import issue with external library
    @Ignore
    @Test
    fun `not jvm imports in js imports`() {
        val code = """
            fun main() {
                val timeZone  = getDefaultTimeZone$CARET_MARKER
            }
        """.trimIndent()

        val foundCompletionsTexts = getCompletions(codeWithCaret = code, isJs = true).map { it.text }
        val completions = listOf(
            "com.fasterxml.jackson.databind.util.StdDateFormat.getDefaultTimeZone()"
        )
        completions.forEach {
            assertFalse(
                foundCompletionsTexts.contains(it),
                "Wrong completion text for import. Expected not to find $it in $foundCompletionsTexts"
            )
        }
    }

    private fun completionContainsCheckOtherImports(
        foundCompletions: List<Completion>,
        completions: List<Pair<String, Boolean>>
    ) {
        val result = foundCompletions.map { Pair(it.displayText, it.hasOtherImports ?: false) }
        assertTrue(result.isNotEmpty())
        completions.forEach { suggest ->
            assertTrue(result.contains(suggest))
        }
    }

    fun getCompletions(codeWithCaret: String, isJs: Boolean = false): List<Completion>

    private fun doImportClassTest(isJs: Boolean = false) {
        performCompletionChecks(
            codeWithCaret = "fun main() {\n    val rand = Random$CARET_MARKER\n}",
            expected = listOf("Random (kotlin.random)"),
            isJs = isJs
        )
    }

    private fun doImportMethodWithOtherImportTest(isJs: Boolean = false) {
        val code = ("""
            import kotlin.math.sin
            fun main() {
                val s = sin$CARET_MARKER
            }
        """.trimIndent())

        val foundCompletions = getCompletions(codeWithCaret = code, isJs = isJs).map { it.displayText }
        val completions = listOf(
            "sin(x: Double) (kotlin.math)",
            "sin(x: Float) (kotlin.math)"
        )
        completions.forEach {
            assertFalse(
                foundCompletions.contains(it),
                "Suggests adding an import, even though it has already been added."
            )
        }
    }

    private fun doImportClassWithParametersTest(isJs: Boolean = false) {
        val code = """
            fun main() {
                randVal = Random$CARET_MARKER(3)
                println(randomVal.nextInt())
            }
        """.trimIndent()

        performCompletionChecks(
            codeWithCaret = code,
            expected = listOf("Random (kotlin.random)"),
            isJs = isJs
        )
    }

    private fun doImportMethodTest(isJs: Boolean = false) {
        val code = """
            fun main() {
                val s = sin$CARET_MARKER
            }
        """.trimIndent()

        performCompletionChecks(
            codeWithCaret = code,
            expected = listOf(
                "sin(x: Double) (kotlin.math)",
                "sin(x: Float) (kotlin.math)"
            ),
            isJs = isJs
        )
    }

    private fun doOpenBracketAfterImportCompletionTest(isJs: Boolean = false) {
        val code = """
            fun main() {
                val s = sin$CARET_MARKER
            }
        """.trimIndent()

        val foundCompletions = getCompletions(codeWithCaret = code, isJs = isJs).map { it }
        val completions = listOf("sin(")
        completions.forEach {
            assertTrue(
                foundCompletions.map { c -> c.text }.contains(it),
                "Wrong completion text for import. Expected to find $it in $foundCompletions"
            )
        }
    }
}