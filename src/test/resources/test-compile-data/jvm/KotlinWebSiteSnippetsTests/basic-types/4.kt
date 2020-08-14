fun main() {
    val b: Byte = 1
//sampleStart
    val i: Int = b.toInt() // OK: explicitly widened
    print(i)
//sampleEnd
}