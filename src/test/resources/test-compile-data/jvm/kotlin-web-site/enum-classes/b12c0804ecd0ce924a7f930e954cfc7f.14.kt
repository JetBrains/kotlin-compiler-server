enum class Priority {
    HIGH, LOW, MEDIUM
}

fun main() {
    println(Priority.entries.sorted())
    // [HIGH, LOW, MEDIUM]
}