fun main() {
//sampleStart
    var arr = intArrayOf(0, 1, 2)

    arr = arr.copyOf(arr.size + 1)
    println(arr.joinToString())
    // 0, 1, 2, 0

    arr[arr.lastIndex] = 3
    println(arr.joinToString())
    // 0, 1, 2, 3
//sampleEnd
}