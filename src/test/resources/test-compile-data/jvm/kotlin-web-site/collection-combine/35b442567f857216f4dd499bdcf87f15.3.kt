fun main() {
//sampleStart
    val numbers = setOf("one", "two", "three")

    // Finds elements present in both collections
    println(numbers intersect setOf("two", "one"))
    // [one, two]
   
    // Excludes elements present in the second collection
    println(numbers subtract setOf("three", "four"))
    // [one, two]

    // Reordering the second collection doesn't change the result
    println(numbers subtract setOf("four", "three"))
    // [one, two]
//sampleEnd
}