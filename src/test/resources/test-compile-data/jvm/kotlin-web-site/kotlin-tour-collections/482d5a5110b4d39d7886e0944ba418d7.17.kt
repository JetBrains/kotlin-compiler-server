fun main() {
//sampleStart
    val readOnlyAccountBalances = mapOf(1 to 100, 2 to 100, 3 to 100)
    println(2 in readOnlyAccountBalances.keys)
    // true
    println(200 in readOnlyAccountBalances.values)
    // false
//sampleEnd
}