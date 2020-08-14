fun main() {
    fun printDouble(d: Double) { print(d) }

    val i = 1    
    val d = 1.1
    val f = 1.1f 

    printDouble(d)
//    printDouble(i) // Error: Type mismatch
//    printDouble(f) // Error: Type mismatch
}