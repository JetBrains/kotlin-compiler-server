fun main() {
//sampleStart
    fun read(
        b: Int,
        print: Unit? = println("No argument passed for 'print'")
    ) { println(b) }
    
    // Prints "No argument passed for 'print'", then "1"
    read(1)
    // Prints only "1"
    read(1, null)
    //sampleEnd
}