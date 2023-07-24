import kotlin.time.*

fun main() {
//sampleStart
   val timeSource = TimeSource.Monotonic
   val mark = timeSource.markNow()
//sampleEnd
}