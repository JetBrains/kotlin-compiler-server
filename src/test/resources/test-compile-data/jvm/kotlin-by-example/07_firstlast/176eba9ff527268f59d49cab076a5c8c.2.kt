fun main() {

//sampleStart
   val words = listOf("foo", "bar", "baz", "faz")         // 1
   val empty = emptyList<String>()                        // 2
   
   val first = empty.firstOrNull()                        // 3
   val last = empty.lastOrNull()                          // 4
   
   val firstF = words.firstOrNull { it.startsWith('f') }  // 5
   val firstZ = words.firstOrNull { it.startsWith('z') }  // 6
   val lastF = words.lastOrNull { it.endsWith('f') }      // 7
   val lastZ = words.lastOrNull { it.endsWith('z') }      // 8
//sampleEnd

   println("First $first, last $last")
   println("First starts with 'f' is $firstF, last starts with 'z' is $firstZ")
   println("First ends with 'f' is $lastF, last ends with 'z' is $lastZ")
}