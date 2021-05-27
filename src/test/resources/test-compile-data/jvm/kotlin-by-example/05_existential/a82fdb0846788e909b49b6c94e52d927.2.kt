fun main() {

//sampleStart
    val numbers = listOf(1, -2, 3, -4, 5, -6)            // 1
    
    val allEven = numbers.all { it % 2 == 0 }            // 2
    
    val allLess6 = numbers.all { it < 6 }                // 3
//sampleEnd

    println("Numbers: $numbers")
    println("All numbers are even: $allEven")
    println("All numbers are less than 6: $allLess6")
}