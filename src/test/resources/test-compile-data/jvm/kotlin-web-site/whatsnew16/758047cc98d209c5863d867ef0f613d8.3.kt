fun main() {
//sampleStart
    println(Regex("(.+)").replace("Kotlin", """\$ $1""")) // $ Kotlin
    println(Regex("(.+)").replaceFirst("1.6.0", """\\ $1""")) // \ 1.6.0
//sampleEnd
}