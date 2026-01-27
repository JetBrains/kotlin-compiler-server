fun main() {
    //sampleStart
    // Extension function on nullable Any
    fun Any?.toString(): String {
        if (this == null) return "null"
        // After null check, `this` is smart-cast to non-nullable Any
        // So this call resolves to the regular toString() function
        return toString()
    }
    
    val number: Int? = 42
    val nothing: Any? = null
    
    println(number.toString())
    // 42
    println(nothing.toString()) 
    // null
    //sampleEnd
}