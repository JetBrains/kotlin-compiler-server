fun someCondition() = true 

fun main() {
//sampleStart
    val d: Int  // 1
    
    if (someCondition()) {
        d = 1   // 2
    } else {
        d = 2   // 2
    }
    
    println(d) // 3
//sampleEnd
}