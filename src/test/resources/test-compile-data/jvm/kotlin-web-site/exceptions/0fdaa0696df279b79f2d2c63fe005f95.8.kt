class MockResource { 
    fun use() { 
        println("Resource being used") 
        // Simulate a resource being used 
        // This throws an ArithmeticException if division by zero occurs
        val result = 100 / 0
        
        // This line is not executed if an exception is thrown
        println("Result: $result") 
    }
    
    fun close() { 
        println("Resource closed") 
    }
}

fun main() { 
    val resource = MockResource()
//sampleStart 
    try {
        
        // Attempts to use the resource 
        resource.use()
        
    } finally {
        
        // Ensures that the resource is always closed, even if an exception occurs 
        resource.close()
    }

    // This line is not printed if an exception is thrown
    println("End of the program")
//sampleEnd
}