fun main() {
    val input: Any = "Hello, Kotlin"

    if (input is String) {
        println("Message length: ${input.length}")
        // Message length: 13
    }

    if (input !is String) { // Same as !(input is String)
        println("Input is not a valid message")
    } else {
        println("Processing message: ${input.length} characters")
        // Processing message: 13 characters
    }
}