data class User(val name: String, val id: Int)

fun main() {
    //sampleStart
    val user = User("Alex", 1)

    // Creates an exact copy of user
    println(user.copy())       
    // User(name=Alex, id=1)

    // Creates a copy of user with name: "Max"
    println(user.copy("Max"))  
    // User(name=Max, id=1)

    // Creates a copy of user with id: 3
    println(user.copy(id = 3)) 
    // User(name=Alex, id=3)
    //sampleEnd
}