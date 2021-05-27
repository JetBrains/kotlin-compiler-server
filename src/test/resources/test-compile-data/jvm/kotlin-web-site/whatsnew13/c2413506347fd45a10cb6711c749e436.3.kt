inline class Name(val s: String)
//sampleStart
fun main() {
    // In the next line no constructor call happens, and
    // at the runtime 'name' contains just string "Kotlin"
    val name = Name("Kotlin")
    println(name.s) 
}
//sampleEnd