enum class Color(val hex: String) {
    RED("#FF0000"),
    GREEN("#00FF00"),
    BLUE("#0000FF")
}

fun main() {
    val color: Color = Color.RED

    println(color)
    // RED
    println(color.hex)
    // #FF0000
    println(Color.GREEN.hex)
    // #00FF00
}