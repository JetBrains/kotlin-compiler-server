fun main() {
//sampleStart
    val simpleArray = arrayOf(1, 2, 3)

    // Randomly shuffles elements
    simpleArray.shuffle()
    println(simpleArray.joinToString())
  
    // Sorts elements
    simpleArray.sort()
    println(simpleArray.joinToString())
    // 1, 2, 3
//sampleEnd
}