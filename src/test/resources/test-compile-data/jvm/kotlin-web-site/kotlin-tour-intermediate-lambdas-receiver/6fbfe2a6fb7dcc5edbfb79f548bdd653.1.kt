fun main() {
    // Lambda expression with receiver definition
    fun StringBuilder.appendText() { append("Hello!") }

    // Use the lambda expression with receiver
    val stringBuilder = StringBuilder()
    stringBuilder.appendText()
    println(stringBuilder.toString())
    // Hello!
}