fun main(){
//sampleStart
  val a: dynamic = "abc"                                               // 1
  val b: String = a                                                    // 2
  
  fun firstChar(s: String) = s[0]
  
  println("${firstChar(a)} == ${firstChar(b)}")                        // 3
  
  println("${a.charCodeAt(0, "dummy argument")} == ${b[0].toInt()}")   // 4
  
  println(a.charAt(1).repeat(3))                                       // 5
  
  fun plus(v: dynamic) = v + 2
  
  println("2 + 2 = ${plus(2)}")                                        // 6
  println("'2' + 2 = ${plus("2")}")
//sampleEnd
}