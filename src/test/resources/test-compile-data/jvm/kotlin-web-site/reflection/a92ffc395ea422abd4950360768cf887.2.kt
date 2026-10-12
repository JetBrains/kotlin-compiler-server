import kotlin.reflect.cast
import kotlin.reflect.safeCast

fun main() {
    val expectedClass = String::class
    val value: Any = "Kotlin"

    // Checks the value without casting it
    println(expectedClass.isInstance(value))
    // true

    // Return the value as a String
    val text = expectedClass.cast(value)
    println(text.length)
    // 6

    // An incompatible cast throws an exception
    val failedCast = runCatching {
        Int::class.cast(value)
    }
    println(failedCast.isFailure)
    // true

    // A safe cast represents the same mismatch with null
    val number = Int::class.safeCast(value)
    println(number)
    // null
}