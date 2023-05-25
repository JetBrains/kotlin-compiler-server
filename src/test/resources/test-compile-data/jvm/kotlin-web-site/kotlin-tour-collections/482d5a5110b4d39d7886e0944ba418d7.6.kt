fun main() { 
//sampleStart
    val shapes: MutableList<String> = mutableListOf("triangle", "square", "circle")
    // Add "pentagon" to the list
    shapes.add("pentagon") 
    println(shapes)  
    // [triangle, square, circle, pentagon]

    // Remove the first "pentagon" from the list
    shapes.remove("pentagon") 
    println(shapes)  
    // [triangle, square, circle]
//sampleEnd
}