fun main() { 
//sampleStart
    val accountBalances: MutableMap<Int, Int> = mutableMapOf(1 to 100, 2 to 100, 3 to 100)
    accountBalances.put(4, 100)  // Add key 4 with value 100 to the list
    println(accountBalances)     // {1=100, 2=100, 3=100, 4=100}
    
    accountBalances.remove(4)    // Remove the key 4 from the list
    println(accountBalances)     // {1=100, 2=100, 3=100}
//sampleEnd
}