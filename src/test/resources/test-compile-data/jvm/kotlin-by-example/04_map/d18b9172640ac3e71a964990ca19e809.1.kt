fun main() {

//sampleStart
    val numbers = listOf(1, -2, 3, -4, 5, -6)     // 1
    
    val doubled = numbers.map { x -> x * 2 }      // 2
    
    val tripled = numbers.map { it * 3 }          // 3
//sampleEnd

    println("Numbers: $numbers")
    println("Doubled Numbers: $doubled")
    println("Tripled Numbers: $tripled")
}