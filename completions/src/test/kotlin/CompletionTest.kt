import base.BaseCompletionTest
import lsp.utils.CARET_MARKER
import org.junit.jupiter.api.Test

interface CompletionTest : BaseCompletionTest {

    @Test
    fun `variable completion test`() {
        performCompletionChecks(
            codeWithCaret = "fun main() {\n    val alex = 1\n    val alex1 = 1 + a$CARET_MARKER\n}",
            expected = listOf(
                "alex"
            )
        )
    }

    @Test
    fun `variable completion test js`() {
        performCompletionChecks(
            codeWithCaret = "fun main() {\n    val alex = 1\n    val alex1 = 1 + a$CARET_MARKER\n}",
            expected = listOf(
                "alex"
            ),
            isJs = true
        )
    }

    @Test
    fun `double to int completion test`() {
        performCompletionChecks(
            codeWithCaret = "fun main() {\n    3.0.toIn$CARET_MARKER\n}",
            expected = listOf(
                "toInt()"
            )
        )
    }

    @Test
    fun `double to int completion test js`() {
        performCompletionChecks(
            codeWithCaret = "fun main() {\n    3.0.toIn$CARET_MARKER\n}",
            expected = listOf(
                "toInt()"
            ),
            isJs = true
        )
    }


    @Test
    fun `listOf completion test`() {
        performCompletionChecks(
            codeWithCaret = "fun main() {\n    list$CARET_MARKER\n}",
            expected = listOf(
                "listOf()",
                "listOf(element: T)",
                "listOfNotNull(element: T?)",
                "listOfNotNull(vararg elements: T?)",
                "listOf(vararg elements: T)"
            )
        )
    }

    @Test
    fun `listOf completion test js`() {
        performCompletionChecks(
            codeWithCaret = "fun main() {\n    list$CARET_MARKER\n}",
            expected = listOf(
                "listOf()",
                "listOf(element: T)",
                "listOfNotNull(element: T?)",
                "listOfNotNull(vararg elements: T?)",
                "listOf(vararg elements: T)"
            ),
            isJs = true
        )
    }
}