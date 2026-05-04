//sampleStart  
fun generalizedEquals(a: Any, b: Any): Boolean {
    return a == b
}

fun main() {
    // Operands statically typed as floating-point numbers
    println(Double.NaN == Double.NaN) // false
    println(0.0 == -0.0) // true

    // Operands used through a non-floating-point static type
    println(generalizedEquals(Double.NaN, Double.NaN)) // true
    println(generalizedEquals(0.0, -0.0)) // false
//sampleEnd  
}