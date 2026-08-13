fun main() {
//sampleStart
    val number = 4
    val isEven = number % 2 == 0

    // Condition already has the `Boolean` type
    // You do not need to compare it to `true` or `false`
    if (isEven) { 
        println("The number is even.")
    } else {
        println("The number is odd.")
    }
//sampleEnd    
}