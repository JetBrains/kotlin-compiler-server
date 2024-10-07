fun main() {
    val num: Int = try {

        // If count() completes successfully, its return value is assigned to num
        count()
        
    } catch (e: ArithmeticException) {
        
        // If count() throws an exception, the catch block returns -1, 
        // which is assigned to num
        -1
    }
    println("Result: $num")
}

// Simulates a function that might throw ArithmeticException
fun count(): Int {
    
    // Change this value to return a different value to num
    val a = 0
    
    return 10 / a
}