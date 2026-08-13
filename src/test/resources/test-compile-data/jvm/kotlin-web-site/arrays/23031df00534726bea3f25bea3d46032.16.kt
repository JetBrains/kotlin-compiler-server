fun main() {
//sampleStart
    // Creates an Int array with 5 elements
    val numbers = intArrayOf(1, 2, 3, 4, 5)
    println(numbers.joinToString())
    // 1, 2, 3, 4, 5

    // Creates a Char array with 3 elements
    val characters = charArrayOf('K', 't', 'l')
    println(characters.joinToString())
    // K, t, l

    // Creates a Double array with 3 elements
    val doubles = doubleArrayOf(0.22, 4.16, 0.5)
    println(doubles.joinToString()) 
    // 0.22, 4.16, 0.5
//sampleEnd
}