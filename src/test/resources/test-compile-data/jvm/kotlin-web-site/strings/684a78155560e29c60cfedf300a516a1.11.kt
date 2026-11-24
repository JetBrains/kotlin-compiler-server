fun main() {
//sampleStart    
    println("abc".compareTo("abd") < 0)
    // true
    
    println("abc".compareTo("ABC") > 0)
    // true
    
    // Pass true to ignore case differences
    println("abc".compareTo("ABC", ignoreCase = true) == 0)
    // true
//sampleEnd  
}