fun main() {
    //sampleStart
    fun log(
        level: Int = 0,
        code:  Int = 1,
        action: () -> Unit,
    ) { println (level)
        println (code)
        action() }
    
    // Passes 1 for 'level' and uses the default value 1 for 'code'
    log(1) { println("Connection established") }
    
    // Uses both default values, 0 for 'level' and 1 for 'code'
    log(action = { println("Connection established") })
    
    // Equivalent to the previous call, uses both default values
    log { println("Connection established") }
    //sampleEnd
}