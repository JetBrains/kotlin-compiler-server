fun main() {
//sampleStart
    val numbers = setOf("one", "two", "three")

    // Swapping the collections changes the result's iteration order
    println(numbers union setOf("four", "five"))
    // [one, two, three, four, five]
    println(setOf("four", "five") union numbers)
    // [four, five, one, two, three]
//sampleEnd
}