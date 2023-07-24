fun main() {
//sampleStart
    // Read-only map
    val readOnlyAccountBalances = mapOf(1 to 100, 2 to 100, 3 to 100)
    println(readOnlyAccountBalances)
    // {1=100, 2=100, 3=100}
    
    // Mutable map with explicit type declaration
    val accountBalances: MutableMap<Int, Int> = mutableMapOf(1 to 100, 2 to 100, 3 to 100)         
    println(accountBalances)
    // {1=100, 2=100, 3=100}
//sampleEnd
}