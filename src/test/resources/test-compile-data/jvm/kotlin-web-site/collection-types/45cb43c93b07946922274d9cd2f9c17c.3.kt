fun main() {
//sampleStart
    val numbers = mutableListOf(1, 2, 3, 4)
    // Adds an element to the list
    numbers.add(5)
    // Removes the element at index 1
    numbers.removeAt(1)
    // Replaces the element at index 0
    numbers[0] = 0
    
    numbers.shuffle()
    println(numbers)
//sampleEnd
}