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

   println("Empty list: first is $first, last is $last")
   println("Word list: first item starting with 'f' is $firstF, first item starting with 'z' is $firstZ")
   println("Word list: last item ending with 'f' is $lastF, last item ending with 'z' is $lastZ")
}