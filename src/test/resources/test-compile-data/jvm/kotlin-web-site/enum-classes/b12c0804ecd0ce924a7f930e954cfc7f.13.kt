enum class Priority {
    LOW, MEDIUM, HIGH
}

fun main() {
    println(Priority.LOW < Priority.HIGH)
    // true
    println(Priority.HIGH > Priority.MEDIUM)
    // true
}