fun main() {
//sampleStart

    val hasErrors = true
    val hasWarnings = true
    val isComplete = false
    
    // buildString creates an empty buffer
    val status = buildString {
        // Appends "Errors found" to the buffer
        if (hasErrors) append("Errors found")
        if (hasWarnings) {
            // The buffer is not empty, appends "; "
            if (isNotEmpty()) append("; ")
            // Appends "Warnings found"
            append("Warnings found")
        }
        // isComplete = false, nothing to append
        if (isComplete) {
            if (isNotEmpty()) append("; ")
            append("Completed")
        }
        // The buffer is not empty, skips the fallback
        if (isEmpty()) append("OK")
    }
    
    println(status)
    // Errors found; Warnings found
//sampleEnd
}