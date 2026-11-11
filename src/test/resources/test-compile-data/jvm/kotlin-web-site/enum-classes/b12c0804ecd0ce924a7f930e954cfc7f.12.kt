enum class Color(val hex: String) {
    RED("#FF0000"),
    GREEN("#00FF00"),
    BLUE("#0000FF")
}

//sampleStart
fun main() {
    val color = Color.RED

    println(color == Color.RED)
    // true
    println(color == Color.BLUE)
    // false
}
//sampleEnd