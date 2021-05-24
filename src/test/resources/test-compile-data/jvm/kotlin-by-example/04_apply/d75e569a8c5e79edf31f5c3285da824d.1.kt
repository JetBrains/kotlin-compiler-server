data class Person(var name: String, var age: Int, var about: String) {
    constructor() : this("", 0, "")
}

fun main() {
//sampleStart
    val jake = Person()                                     // 1
    val stringDescription = jake.apply {                    // 2
        name = "Jake"                                       // 3
        age = 30
        about = "Android developer"
    }.toString()                                            // 4
//sampleEnd
    println(stringDescription)
}