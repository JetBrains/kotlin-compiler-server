fun main() {
//sampleStart
    // Kotlin
    val regex = Regex("""\w*\d+\w*""") // raw string
    val input = "login: Pokemon5, password: 1q2w3e4r5t"
    val replacementResult = regex.replace(input, replacement = "xxx")
    println("Initial input: '$input'")
    println("Anonymized input: '$replacementResult'")
//sampleEnd
}