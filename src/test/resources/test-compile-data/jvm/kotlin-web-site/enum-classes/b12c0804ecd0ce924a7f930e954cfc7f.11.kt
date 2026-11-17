import kotlin.enums.enumEntries

enum class RGB { RED, GREEN, BLUE }

inline fun <reified T : Enum<T>> printAllValues() {
    println(enumEntries<T>().joinToString { it.name })
}

inline fun <reified T : Enum<T>> findByName(name: String): T = enumValueOf<T>(name)

fun main() {
    printAllValues<RGB>()
    // RED, GREEN, BLUE
    println(findByName<RGB>("GREEN"))
    // GREEN
}