fun main() {
    val rawInput: Any = "user-1234"

    // Casts to String successfully
    val userId = rawInput as? String
    println("Logging in user with ID: $userId")
    // Logging in user with ID: user-1234

    // Assigns a null value to wrongCast
    val wrongCast = rawInput as? Int
    println("wrongCast contains: $wrongCast")
    // wrongCast contains: null
}