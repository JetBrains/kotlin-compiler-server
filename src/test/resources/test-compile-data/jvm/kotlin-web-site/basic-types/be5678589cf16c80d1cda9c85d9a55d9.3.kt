fun main() {
//sampleStart
    val b: Byte = 1 // OK, literals are checked statically
    // val i: Int = b // ERROR
    val i1: Int = b.toInt()
//sampleEnd
}