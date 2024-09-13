fun main() {
//sampleStart
    // Assigns null to a nullable variable  
    val b: String? = null
    // Checks for nullability. If not null, returns length. If null, returns 0
    val l: Int = if (b != null) b.length else 0
    println(l)
    // 0
//sampleEnd
}