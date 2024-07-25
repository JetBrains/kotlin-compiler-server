fun main() {
    val nullString: String? = null
    println(nullString?.length ?: 0)
    // 0
}