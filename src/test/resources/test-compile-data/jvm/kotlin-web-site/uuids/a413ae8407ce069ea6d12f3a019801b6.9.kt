import kotlin.uuid.Uuid

fun main() {
//sampleStart 
    val id = Uuid.random()

    val bytes = id.toByteArray()
    val original = Uuid.fromByteArray(bytes)
  
    println(id)
    
    println(bytes)
    println(original)

    println(id == original) 
    // true
//sampleEnd  
}