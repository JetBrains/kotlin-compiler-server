class Language(val name: String) {
    fun printName() {
        println(this.name)
    }
}

fun main() {
    val language = Language("Kotlin")
    language.printName()
    // Kotlin
}