enum class Color(val rgb: Int) {
    RED(0xFF0000),
    GREEN(0x00FF00),
    BLUE(0x0000FF)
}

//sampleStart
fun main() {
    val color = Color.entries.first { it.rgb == 0xFF0000 }

    println(color)
    // RED
}
//sampleEnd