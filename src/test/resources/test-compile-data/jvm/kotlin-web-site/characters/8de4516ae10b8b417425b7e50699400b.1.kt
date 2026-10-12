fun main() {
//sampleStart
    val myChar = 'A'
    // Checks if the character represents a digit
    println(myChar.isDigit()) // false
    // Checks if the character represents an uppercase letter
    println(myChar.isUpperCase()) // true
    // Returns a lowercase version
    println(myChar.lowercaseChar()) // 'a'
//sampleEnd
}