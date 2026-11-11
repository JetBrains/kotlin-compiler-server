fun main () {
//sampleStart    
fun greeting(
    userId: Int = 0,
    message: () -> Unit,
)
{ println(userId)
  message() }
    
// Uses the default value for 'userId'
greeting() { println ("Hello!") }
// 0
// Hello!
//sampleEnd
}