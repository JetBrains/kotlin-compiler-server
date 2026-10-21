fun main() { 
//sampleStart
    val domain = "kotlinlang.org"
    
    // Checks if the string contains "."
    println(domain.contains("."))
    // true
    
    // Checks if the string starts with "kotlin"
    println(domain.startsWith("kotlin"))
    // true
    
    // Checks if the string ends with ".org"
    println(domain.endsWith(".org"))
    // true
//sampleEnd
}