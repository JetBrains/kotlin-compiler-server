import kotlin.uuid.Uuid

fun main() {
//sampleStart 
    val id = Uuid.random()
    
    id.toLongs { mostSignificantBits, leastSignificantBits ->
        println(mostSignificantBits)
        println(leastSignificantBits)
    }
//sampleEnd  
}