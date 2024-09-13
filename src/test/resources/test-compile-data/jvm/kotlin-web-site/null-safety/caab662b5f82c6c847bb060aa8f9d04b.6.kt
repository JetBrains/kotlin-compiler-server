fun main() {
//sampleStart
    // Assigns null to a nullable variable  
    val b: String? = null
    // Checks for nullability. If not null, returns length. If null, returns a non-null value
    val l = b?.length ?: 0
    println(l)
    // 0
//sampleEnd
}