enum class Color(val hex: String) {
    RED("#FF0000"),
    GREEN("#00FF00"),
    BLUE("#0000FF")
}

//sampleStart
fun printColor(color: Color) {
    println("Color: $color")
    println("Hex code: ${color.hex}")
}

fun main() {
    printColor(Color.BLUE)
    // Color: BLUE
    // Hex code: #0000FF
}
//sampleEnd