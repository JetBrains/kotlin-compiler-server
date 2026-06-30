fun main() {
//sampleStart
    val pairArray = arrayOf("apple" to 120, "banana" to 150, "cherry" to 90, "apple" to 140)

    // Converts to a Map
    // Fruits are keys, calorie numbers are values
    // The latest "apple" value overwrites the first one
    println(pairArray.toMap())
    // {apple=140, banana=150, cherry=90}
//sampleEnd
}