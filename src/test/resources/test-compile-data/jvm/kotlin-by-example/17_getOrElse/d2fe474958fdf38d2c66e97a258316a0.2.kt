fun main() {

//sampleStart
    val map = mutableMapOf<String, Int?>()
    println(map.getOrElse("x") { 1 })       // 1
    
    map["x"] = 3
    println(map.getOrElse("x") { 1 })       // 2
    
    map["x"] = null
    println(map.getOrElse("x") { 1 })       // 3
//sampleEnd
}