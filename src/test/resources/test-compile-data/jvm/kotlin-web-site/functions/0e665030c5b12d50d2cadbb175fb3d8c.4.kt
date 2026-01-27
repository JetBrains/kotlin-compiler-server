class MyStringCollection {
    val items = mutableListOf<String>()

    infix fun add(s: String) {
        println("Adding: $s")
        items += s
    }

    fun build() {
        add("first")      // Correct: ordinary function call
        this add "second" // Correct: infix call with an explicit receiver
        // add "third"    // Compiler error: needs an explicit receiver
    }

    fun printAll() = println("Items = $items")
}

fun main() {
    val myStrings = MyStringCollection()
    // Adds "first" and "second" to the list twice
    myStrings.build()
      
    myStrings.printAll()
    // Adding: first
    // Adding: second
    // Items = [first, second]
}