fun main() {
//sampleStart
    // Includes both 1 and 4
    println(4 in 1..4)
    // true
    
    // Includes 1, excludes 4
    println(4 in 1..<4)
    // false
//sampleEnd
}