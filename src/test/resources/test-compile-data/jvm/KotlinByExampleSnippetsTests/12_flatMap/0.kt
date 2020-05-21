fun main() {

//sampleStart
    val numbers = listOf(1, 2, 3)                        // 1

    val tripled = numbers.flatMap { listOf(it, it, it) } // 2
//sampleEnd

    println("Numbers: $numbers")
    println("Transformed: $tripled")
}