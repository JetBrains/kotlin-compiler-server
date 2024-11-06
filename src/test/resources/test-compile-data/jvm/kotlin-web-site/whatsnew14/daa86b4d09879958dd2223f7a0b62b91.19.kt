fun main() {
//sampleStart
    val sb = StringBuilder("Bye Kotlin 1.3.72")
    sb.deleteRange(0, 3)
    sb.insertRange(0, "Hello", 0 ,5)
    sb.set(15, '4')
    sb.setRange(17, 19, "0")
    print(sb.toString())
//sampleEnd
}