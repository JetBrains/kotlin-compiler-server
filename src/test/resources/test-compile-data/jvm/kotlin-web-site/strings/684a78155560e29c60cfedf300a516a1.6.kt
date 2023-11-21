fun main() { 
//sampleStart
    // Formats to add zeroes and make a length of seven
    val integerNumber = String.format("%07d", 31416)
    println(integerNumber)
    // 0031416

    // Formats with four decimals and sign
    val floatNumber = String.format("%+.4f", 3.141592)
    println(floatNumber)
    // +3.1416

    // Formats with uppercase for two placeholders
    val helloString = String.format("%S %S", "hello", "world")
    println(helloString)
    // HELLO WORLD
//sampleEnd    
}