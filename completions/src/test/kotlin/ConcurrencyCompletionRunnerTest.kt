import base.BaseCompletionTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import lsp.utils.CARET_MARKER
import org.junit.jupiter.api.Test
import kotlin.test.Ignore

abstract class ConcurrencyCompletionRunnerTest : BaseCompletionTest {
    // TODO(Dmitrii Krasnov): this test is disabled until KTL-2807 is fixed
    @Ignore
    @Test
    fun `a lot of complete test`() {
        runManyTest {
            performCompletionChecks(
                codeWithCaret = "fun main() {\n    val alex = 1\n    val alex1 = 1 + a$CARET_MARKER\n}",
                expected = listOf(
                    "alex"
                )
            )
        }
    }

    // TODO(Dmitrii Krasnov): this test is disabled until KTL-2807 is fixed
    @Ignore
    @Test
    fun `a lot of complete test JS`() {
        runManyTest {
            performCompletionChecks(
                codeWithCaret = "fun main() {\n    val alex = 1\n    val alex1 = 1 + a$CARET_MARKER\n}",
                expected = listOf(
                    "alex"
                ),
                isJs = true
            )
        }
    }

    private fun runManyTest(times: Int = 100, test: () -> Unit) {
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