fun main() {
//sampleStart
    val arr = IntArray(3)
    println(arr.joinToString())
    // 0, 0, 0
    
   arr.fill(1)
   println(arr.joinToString())
   // 1, 1, 1
  
   arr.fill(0, 0, 2)
   println(arr.joinToString())
   // 0, 0, 1
//sampleEnd
}