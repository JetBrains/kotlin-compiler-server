fun main() {

//sampleStart
    val numbers = listOf(1, -2, 3, -4, 5, -6)            // 1
    
    val allEven = numbers.none { it % 2 == 1 }           // 2
    
    val allLess6 = numbers.none { it > 6 }               // 3
//sampleEnd

    println("Numbers: $numbers")
    println("All numbers are even: $allEven")
    println("No element greater than 6: $allLess6")
}