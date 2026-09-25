// UserService defines the dependency required in context 
interface UserService {
    fun log(message: String)
    fun findUserById(id: Int): String
}

// Declares a function with a context parameter
context(users: UserService)
fun outputMessage(message: String) {
    // Uses log from the context
    users.log("Log: $message")
}

// Declares a property with a context parameter
context(users: UserService)
val firstUser: String
    // Uses findUserById from the context    
    get() = users.findUserById(1)

fun main() {
    val users = object : UserService {
        override fun log(message: String) {
            println(message)
        }

        override fun findUserById(id: Int): String {
            return "User $id"
        }
    }

    context(users) {
        outputMessage("Looking up the first user")
        println(firstUser)
        // User 1
    }
}