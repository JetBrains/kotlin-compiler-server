fun main() {
//sampleStart
    // Kotlin
    // The same code as in Java:
    val numbers = listOf(1, 2)
    
    println(numbers[0])  // Can throw IndexOutOfBoundsException if the collection is empty
    //numbers.get(5)     // Exception!

    // More abilities:
    println(numbers.firstOrNull())
    println(numbers.getOrNull(5)) // null
//sampleEnd
}