fun main() {
//sampleStart
    println("""
     Not
     trimmed
     text
     """
    )

    println("""
     Trimmed
     text
     """.trimIndent()
    )

    println()

    val a = """Trimmed to margin text:
            |if(a > 1) {
            |    return a
            |}""".trimMargin()

   println(a)
//sampleEnd
}