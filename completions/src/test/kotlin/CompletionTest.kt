import base.BaseCompletionTest
import org.junit.jupiter.api.Test

interface CompletionTest : BaseCompletionTest {

    @Test
    fun `variable completion test`() {
        performCompletionChecks(
            code = "fun main() {\n    val alex = 1\n    val alex1 = 1 + a\n}",
            line = 2,
            character = 21,
            expected = listOf(
                "alex"
            )
        )
    }

    @Test
    fun `variable completion test js`() {
        performCompletionChecks(
            code = "fun main() {\n    val alex = 1\n    val alex1 = 1 + a\n}",
            line = 2,
            character = 21,
            expected = listOf(
                "alex"
            ),
            isJs = true
        )
    }

    @Test
    fun `double to int completion test`() {
        performCompletionChecks(
            code = "fun main() {\n    3.0.toIn\n}",
            line = 1,
            character = 12,
            expected = listOf(
                "toInt()"
            )
        )
    }

    @Test
    fun `double to int completion test js`() {
        performCompletionChecks(
            code = "fun main() {\n    3.0.toIn\n}",
            line = 1,
            character = 12,
            expected = listOf(
                "toInt()"
            ),
            isJs = true
        )
    }


    @Test
    fun `listOf completion test`() {
        performCompletionChecks(
            code = "fun main() {\n    list\n}",
            line = 1,
            character = 8,
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
            code = "fun main() {\n    list\n}",
            line = 1,
            character = 8,
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