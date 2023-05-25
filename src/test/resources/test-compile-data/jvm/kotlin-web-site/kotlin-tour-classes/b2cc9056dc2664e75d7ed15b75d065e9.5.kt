data class User(val name: String, val id: Int)

fun main() {
    //sampleStart
    val user = User("Alex", 1)
    val secondUser = User("Alex", 1)
    val thirdUser = User("Max", 2)

    // Compares user to second user
    println("user == secondUser: ${user == secondUser}") 
    // user == secondUser: true
    
    // Compares user to third user
    println("user == thirdUser: ${user == thirdUser}")   
    // user == thirdUser: false
    //sampleEnd
}