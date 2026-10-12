fun main() {
//sampleStart
    // Creates an Int array of size 5 with the values initialized to zero
    val primitiveTypeArray = IntArray(5)
    println(primitiveTypeArray.joinToString())
    // 0, 0, 0, 0, 0

    // Creates an Int array and takes an initializer function
    val squares = IntArray(5) { i -> i * i }
    println(squares.joinToString())
    // 0, 1, 4, 9, 16
//sampleEnd
}