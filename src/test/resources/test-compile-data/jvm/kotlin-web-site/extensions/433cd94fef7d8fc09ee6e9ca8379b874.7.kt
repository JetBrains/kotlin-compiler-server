fun main() {
//sampleStart
    open class Shape
    class Rectangle: Shape()
    
    fun Shape.getName() = "Shape"
    fun Rectangle.getName() = "Rectangle"
    
    fun printClassName(shape: Shape) {
        println(shape.getName())
    }
    
    printClassName(Rectangle())
    // Shape
//sampleEnd
}