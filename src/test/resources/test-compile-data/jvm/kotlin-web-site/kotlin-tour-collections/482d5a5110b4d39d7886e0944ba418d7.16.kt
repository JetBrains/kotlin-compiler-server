fun main() { 
//sampleStart
    val readOnlyAccountBalances = mapOf(1 to 100, 2 to 100, 3 to 100)
    println(readOnlyAccountBalances.keys)
    // [1, 2, 3]
    println(readOnlyAccountBalances.values)
    // [100, 100, 100]
//sampleEnd
}