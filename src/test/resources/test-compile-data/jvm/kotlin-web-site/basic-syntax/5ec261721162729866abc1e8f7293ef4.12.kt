fun main() {
//sampleStart
    // Initializes the variable x at the moment of declaration; type is not required
    val x = 5
    // Declares the variable c without initialization; type is required
    val c: Int
    // Initializes the variable c after declaration 
    c = 3
    // 5 
    // 3
//sampleEnd
    println(x)
    println(c)
}