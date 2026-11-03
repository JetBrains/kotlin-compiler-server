fun main() {
//sampleStart
    val numbers = listOf("one", "two", "three", "four")

    val plusList = numbers + "five"
    val minusList = numbers - listOf("three", "four")
    
    println(plusList)
    // [one, two, three, four, five]
   
    println(minusList)
    // [one, two]
//sampleEnd
}