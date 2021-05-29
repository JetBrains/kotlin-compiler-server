fun main() {

//sampleStart
    val words = listOf("Lets", "find", "something", "in", "collection", "somehow")  // 1
    
    val first = words.find { it.startsWith("some") }                                // 2
    val last = words.findLast { it.startsWith("some") }                             // 3
    
    val nothing = words.find { it.contains("nothing") }                             // 4
//sampleEnd

    println("The first word starting with \"some\" is \"$first\"")
    println("The last word starting with \"some\" is \"$last\"")
    println("The first word containing \"nothing\" is ${nothing?.let { "\"$it\"" } ?: "null"}")
}