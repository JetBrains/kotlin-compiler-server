fun customPrint(s: String) {
    print(s.toUpperCase())
}

fun main() {
//sampleStart
    val empty = "test".let {               // 1
        customPrint(it)                    // 2
        it.isEmpty()                       // 3
    }
    println(" is empty: $empty")


    fun printNonNull(str: String?) {
        println("Printing \"$str\":")

        str?.let {                         // 4
            print("\t")
            customPrint(it)
            println()
        }
    }
    printNonNull(null)
    printNonNull("my string") 
//sampleEnd
}