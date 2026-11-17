fun main() {
//sampleStart
    var a = 'A'
    
    a += 10
    println(a)   // 'K'
    
    println(++a) // 'L'  prefix increment
    println(a++) // 'L'  postfix increment
    println(a)   // 'M'
    
    println(--a) // 'L'  prefix decrement
    println(a--) // 'L'  postfix decrement
    println(a)   // 'K'
//sampleEnd
}