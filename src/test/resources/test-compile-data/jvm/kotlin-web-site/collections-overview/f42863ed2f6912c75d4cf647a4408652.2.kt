fun printAll(strings: Collection<String>) {
        for(s in strings) print("$s ")
        println()
    }
    
fun main() {
    val stringList = listOf("one", "two", "one")
    printAll(stringList)
    
    val stringSet = setOf("one", "two", "three")
    printAll(stringSet)
}