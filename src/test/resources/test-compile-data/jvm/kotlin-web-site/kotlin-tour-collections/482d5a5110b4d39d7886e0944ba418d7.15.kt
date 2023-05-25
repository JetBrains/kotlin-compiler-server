fun main() { 
//sampleStart
    val readOnlyAccountBalances = mapOf(1 to 100, 2 to 100, 3 to 100)
    println(readOnlyAccountBalances.containsKey(2))
    // true
//sampleEnd
}