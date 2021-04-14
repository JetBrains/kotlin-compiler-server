fun main() {

//sampleStart
    val numbers = listOf(1, 2, 3)
    val empty = emptyList<Int>()

    println("Numbers: $numbers, min = ${numbers.minOrNull()} max = ${numbers.maxOrNull()}") // 1
    println("Empty: $empty, min = ${empty.minOrNull()}, max = ${empty.maxOrNull()}")        // 2
//sampleEnd
}