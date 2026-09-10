fun String.lastCharacter(): Char {
    println(this.length)
    // 6
    return this[this.length - 1]
}

fun main() {
    println("Kotlin".lastCharacter())
    // n
}