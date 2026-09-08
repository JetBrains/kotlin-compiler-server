enum class Color(val rgb: Int) {
    RED(0xFF0000),
    GREEN(0x00FF00),
    BLUE(0x0000FF);

    companion object {
        fun fromName(name: String): Color? =
            entries.find { it.name == name }

        fun fromPosition(position: Int): Color? =
            entries.getOrNull(position)

        fun fromRgb(rgb: Int): Color? =
            entries.find { it.rgb == rgb }
    }
}

fun main() {
    println(Color.fromName("RED"))
    // RED
    println(Color.fromPosition(1))
    // GREEN
    println(Color.fromRgb(0x0000FF))
    // BLUE
    println(Color.fromRgb(0xABCDEF))
    // null
}