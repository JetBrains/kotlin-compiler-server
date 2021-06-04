fun main() {
//sampleStart
    // Kotlin
    val numbers = mutableListOf(1, 2, 3, 4, 5, 6)
    val oddNumbers = numbers
        .filter { it % 2 != 0 }
        .joinToString{ "${it.unaryMinus()}" }
  println(oddNumbers)
//sampleEnd
}