import base.BaseCompletionTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import lsp.utils.CARET_MARKER
import org.junit.jupiter.api.Test

abstract class ConcurrencyCompletionRunnerTest : BaseCompletionTest {

    private val numberOfTests = 100

    private val code = """
        fun main() {
            val a = 3.0.toIn$CARET_MARKER
        }
    """.trimIndent()

    private val expectedCompletions = listOf(
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
