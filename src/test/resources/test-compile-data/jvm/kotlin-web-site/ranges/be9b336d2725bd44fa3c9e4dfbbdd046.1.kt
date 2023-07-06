fun main() {
//sampleStart
    // Closed-ended range
    println(4 in 1..4)
    // true
    
    // Open-ended range
    println(4 in 1..<4)
    // false
//sampleEnd
}