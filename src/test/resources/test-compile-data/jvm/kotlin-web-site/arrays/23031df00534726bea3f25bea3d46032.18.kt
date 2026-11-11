fun main() {
//sampleStart
    var arr = intArrayOf(0, 1, 2)

    arr += 3
    println(arr.joinToString())
    // 0, 1, 2, 3

    arr = arr + intArrayOf(4, 5)
    println(arr.joinToString())
    // 0, 1, 2, 3, 4, 5
//sampleEnd
}