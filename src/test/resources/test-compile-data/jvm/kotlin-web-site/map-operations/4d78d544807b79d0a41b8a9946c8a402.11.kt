@OptIn(ExperimentalStdlibApi::class)
fun main() {
//sampleStart
    val mapForNull = mutableMapOf<String, Int?>("one" to null)
    val mapForMissing = mutableMapOf<String, Int?>("one" to null)

    // Replaces the value if "one" has a null value
    mapForNull.getOrPutIfNull("one") { 1 }

    println(mapForNull)
    // {one=1}

    // Keeps the null value because "one" exists in the map
    mapForMissing.getOrPutIfMissing("one") { 1 }

    println(mapForMissing)
    // {one=null}
//sampleEnd
}