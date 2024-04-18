package com.compiler.server

import com.compiler.server.base.BaseExecutorTest
import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SwiftConverterTest : BaseExecutorTest() {

    private fun exactTest(input: String, expected: String) {
        val actual = translateToSwift(input)
        assertEquals(expected, actual.swiftCode.trimEnd())
    }

    private fun containsTest(input: String, expected: String) {
        val actual = translateToSwift(input)
        assertContains(actual.swiftCode.trimEnd(), expected)
    }

    private fun shouldFailTest(input: String) {
        val actual = translateToSwift(input)
        assertTrue(actual.hasErrors())
    }

    @Test
    fun basicSwiftExportTest() = containsTest(
        input = """
            fun main() {}
        """.trimIndent(),
        expected = "public func main() -> Swift.Void"
    )

    @Test
    fun `use stdlib declaration`() = containsTest(
        input = "fun foo(): UInt = 42",
        expected = """
            public func foo() -> Swift.UInt32 {
                fatalError()
            }
        """.trimIndent()
    )

    @Test
    fun `class declaration`() = exactTest(
        input = "public class MyClass { public fun A() {}}",
        expected = """
        import KotlinRuntime

        public class MyClass : KotlinRuntime.KotlinBase {
            public override init() {
                fatalError()
            }
            public func A() -> Swift.Void {
                fatalError()
            }
        }
        """.trimIndent()
    )

    @Test
    fun `simple packages`() = exactTest(
        input = """
            package foo.bar
            
            val myProperty: Int = 42
        """.trimIndent(),
        expected = """
            public extension Playground.foo.bar {
                public static var myProperty: Swift.Int32 {
                    get {
                        fatalError()
                    }
                }
            }
            public enum foo {
                public enum bar {
                }
            }
        """.trimIndent()
    )

    @Test
    fun `invalid code`() = exactTest(
        input = "abracadabra",
        expected = """
        """.trimIndent()
    )

    @Test
    fun `more invalid code`() = exactTest(
        input = "fun foo(): Bar = error()",
        expected = """
            public func foo() -> ERROR_TYPE {
                fatalError()
            }
        """.trimIndent()
    )
}