fun main() {
//sampleStart
    // Assigns a nullable string to a variable  
    val b: String? = "Kotlin"

    // Checks for nullability first and then accesses length
    if (b != null && b.length > 0) {
        print("String of length ${b.length}")
    // Provides alternative if the condition is not met  
    } else {
        print("Empty string")
        // String of length 6
    }
//sampleEnd
}