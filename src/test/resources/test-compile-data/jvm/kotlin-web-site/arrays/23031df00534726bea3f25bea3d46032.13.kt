fun main() {
//sampleStart
    var riversArray = arrayOf("Nile", "Amazon", "Yangtze")

    // Using the += assignment operation creates a new riversArray,
    // copies over the original elements and adds "Mississippi"
    riversArray += "Mississippi"
    println(riversArray.joinToString())
    // Nile, Amazon, Yangtze, Mississippi
//sampleEnd
}