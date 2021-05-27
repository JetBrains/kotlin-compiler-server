fun main() {
//sampleStart
    val b: Int = 10000
    println(b == b) // Prints 'true'
    val boxedB: Int? = b
    val anotherBoxedB: Int? = b
    println(boxedB == anotherBoxedB) // Prints 'true'
//sampleEnd
}