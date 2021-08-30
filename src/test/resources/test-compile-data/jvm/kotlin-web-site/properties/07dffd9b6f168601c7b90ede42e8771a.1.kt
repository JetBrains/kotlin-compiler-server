//sampleStart
class Rectangle(val width: Int, val height: Int) {
    val square: Int
        get() = this.width * this.height
}
//sampleEnd
fun main() {
    val rectangle = Rectangle(3, 4)
    println("Width=${rectangle.width}, height=${rectangle.height}, square=${rectangle.square}")
}