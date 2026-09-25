//sampleStart
fun List<Any>.log() {
    println(joinToString(" | ") { it::class.simpleName ?: "Unknown" })
}

fun main() {
    listOf(1, 2).log()
    // Int | Int
    
    listOf(1L, 2L).log()
    // Long | Long
    
    // Compiler interprets 1 as an ILT and resolves it to Long
    listOf(1, 2L).log()
    // Long | Long
    
    // .toInt() converts the literal to Int
    listOf(1.toInt(), 2L).log()
    // Int | Long
}
//sampleEnd