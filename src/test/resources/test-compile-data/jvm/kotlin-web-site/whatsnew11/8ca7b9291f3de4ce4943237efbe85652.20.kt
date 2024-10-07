fun main(args: Array<String>) {
//sampleStart
    val array = arrayOf("a", "b", "c")
    println(array.toString().substringBefore('@'))  // JVM implementation: type-and-hash gibberish
    println(array.contentToString())  // nicely formatted as list
//sampleEnd
}