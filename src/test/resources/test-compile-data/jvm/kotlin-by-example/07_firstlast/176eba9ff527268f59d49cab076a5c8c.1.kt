fun main() {

//sampleStart
    val numbers = listOf(1, -2, 3, -4, 5, -6)            // 1
    
    val first = numbers.first()                          // 2
    val last = numbers.last()                            // 3
    
    val firstEven = numbers.first { it % 2 == 0 }        // 4
    val lastOdd = numbers.last { it % 2 != 0 }           // 5
//sampleEnd

    println("Numbers: $numbers")
    println("First $first, last $last, first even $firstEven, last odd $lastOdd")
}