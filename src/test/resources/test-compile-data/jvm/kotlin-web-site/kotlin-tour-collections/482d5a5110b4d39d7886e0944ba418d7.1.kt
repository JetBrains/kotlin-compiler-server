fun main() { 
//sampleStart
    // Read only list
    val readOnlyShapes = listOf("triangle", "square", "circle")
    // Mutable list with explicit type declaration
    val shapes: MutableList<String> = mutableListOf("triangle", "square", "circle") 
//sampleEnd
}