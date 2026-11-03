fun main() {
//sampleStart
    // LinkedHashSet is the default implementation
    val numbers = setOf(1, 2, 3, 4)
    val numbersBackwards = setOf(4, 3, 2, 1)
    
    println(numbers.first() == numbersBackwards.first())
    // false
    
    println(numbers.first() == numbersBackwards.last())
    // true
//sampleEnd
}