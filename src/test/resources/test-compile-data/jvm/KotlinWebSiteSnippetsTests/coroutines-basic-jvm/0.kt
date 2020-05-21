import kotlinx.coroutines.*

fun main(args: Array<String>) {
//sampleStart
    println("Start")

    // Start a coroutine
    GlobalScope.launch {
        delay(1000)
        println("Hello")
    }

    Thread.sleep(2000) // wait for 2 seconds
    println("Stop")
//sampleEnd
}