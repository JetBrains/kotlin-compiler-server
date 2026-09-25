import kotlin.uuid.Uuid

fun main() {
//sampleStart    
    val id = Uuid.parse("de2bc56c-ea73-4f3c-8a37-5a46fdb2d79a")
    
    println(id.toString())
    // de2bc56c-ea73-4f3c-8a37-5a46fdb2d79a
    println(id.toHexDashString())
    // de2bc56c-ea73-4f3c-8a37-5a46fdb2d79a
    println(id.toHexString())
    // de2bc56cea734f3c8a375a46fdb2d79a 
//sampleEnd    
}