fun main() {
//sampleStart
       // Kotlin
       var counter = 5;
       val countDown = buildString {
           repeat(5) {
               append(counter--)
               appendLine()
           }
       }
       println(countDown)
//sampleEnd
}