import java.io.File
import java.io.IOException

//sampleStart
fun main() {
    val userSettings = try {
        File("user-settings.json").readText()
    
    // Catches IOException without using the exception instance
    } catch (_: IOException) {
        // Uses a fallback value if loading the file fails
        "{}"
    }

    println(userSettings)
}
//sampleEnd