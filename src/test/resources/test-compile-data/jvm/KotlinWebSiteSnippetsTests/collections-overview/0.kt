fun main() {
//sampleStart
    val numbers = mutableListOf("one", "two", "three", "four")
    numbers.add("five")   // this is OK    
    //numbers = mutableListOf("six", "seven")      // compilation error
//sampleEnd
}