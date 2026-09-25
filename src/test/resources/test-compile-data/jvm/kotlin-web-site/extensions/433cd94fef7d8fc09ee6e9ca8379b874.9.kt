fun main() {
//sampleStart
    class Example {
        fun printFunctionType() { println("Member function") }
    }
    
    // Same name but different signature
    fun Example.printFunctionType(index: Int) { println("Extension function #$index") }
    
    Example().printFunctionType(1)
    // Extension function #1
//sampleEnd
}