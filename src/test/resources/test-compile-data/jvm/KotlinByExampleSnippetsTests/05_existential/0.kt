fun main() {

//sampleStart
    val numbers = listOf(1, -2, 3, -4, 5, -6)            // 1
    
    val anyNegative = numbers.any { it < 0 }             // 2
    
    val anyGT6 = numbers.any { it > 6 }                  // 3
//sampleEnd

    println("Numbers: $numbers")
    println("Is there any number less than 0: $anyNegative")
    println("Is there any number greater than 6: $anyGT6")
}