import kotlin.time.measureTime

fun main() {
//sampleStart
    val timeTaken = measureTime {
        Thread.sleep(100)
    }
    println(timeTaken) // e.g. 103 ms
//sampleEnd
}