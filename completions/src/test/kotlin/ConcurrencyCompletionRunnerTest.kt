import base.BaseCompletionTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import lsp.utils.CARET_MARKER
import org.junit.jupiter.api.Test

interface ConcurrencyCompletionRunnerTest : BaseCompletionTest {

    val numberOfTests: Int
        get() = 100

    val code: String
        get() = """
            fun main() {
                val a = 3.0.toIn$CARET_MARKER
            }
            """.trimIndent()

    val expectedCompletions: List<String>
        get() = listOf(
            "toInt()",
            "toUInt()",
        )

    @Test
    fun `a lot of complete test`() {
        runManyTest(numberOfTests) {
            performCompletionChecks(code, expectedCompletions)
        }
    }

    @Test
    fun `a lot of complete test JS`() {
        runManyTest(numberOfTests) {
            performCompletionChecks(code, expectedCompletions, isJs = true)
        }
    }

    private fun runManyTest(times: Int, test: () -> Unit) {
        runBlocking {
            launch(Dispatchers.IO) {
                repeat(times) {
                    launch(Dispatchers.IO) {
                        test()
                    }
                }
            }.join()
        }
    }
}
