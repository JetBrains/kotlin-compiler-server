fun main() {
//sampleStart
    open class Shape
    
    class Rectangle: Shape()
    
    fun Shape.getName() = "Shape"
    
    fun Rectangle.getName() = "Rectangle"
    
    fun printClassName(s: Shape) {
        println(s.getName())
    }    
    
    printClassName(Rectangle())
//sampleEnd
}