import kotlin.io.path.*
import kotlin.test.Test
import kotlin.test.assertEquals

class SwiftExportTests {

    private fun testSources(input: String, expect: String) {
        val tempDir = createTempDirectory()

        val inputFormatted = input.trimIndent().trimEnd()

        val inputFile = (tempDir / "input.kt").also { it.writeText(inputFormatted) }

        val actual = runSwiftExport(
            sourceFile = inputFile,
            stdlibPath = null,
        )
        val expectFormatted = expect.trimIndent().trimEnd()

        assertEquals(expectFormatted, actual)
    }

    @Test
    fun smoke() = testSources(
        """
            fun foo(): Int = 5
        """,
        """
            public func foo() -> Swift.Int32 {
                stub()
            }
        """
    )

    @Test
    fun `class declaration`() = testSources(
        """
            class A
        """.trimIndent(),
        """
            import KotlinRuntime

            public class A : KotlinRuntime.KotlinBase {
                public override init() {
                    stub()
                }
            }
        """.trimIndent()
    )

    @Test
    fun `object declaration`() = testSources(
        """
            object O
        """.trimIndent(),
        """
            import KotlinRuntime

            public class O : KotlinRuntime.KotlinBase {
                public static var shared: Playground.O {
                    get {
                        stub()
                    }
                }
                private override init() {
                    stub()
                }
            }
        """.trimIndent()
    )

    @Test
    fun `typealias to basic type declaration`() = testSources(
        """
            typealias MyInt = Int
        """.trimIndent(),
        """
            public typealias MyInt = Swift.Int32
        """.trimIndent()
    )
}