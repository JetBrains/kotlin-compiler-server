var greeting = "Hello"

class User(var name: String)

fun main() {
    // A top-level property reference
    // No receiver is needed
    println(::greeting.get())
    // Hello

    ::greeting.set("Hi")
    println(greeting)
    // Hi

    val user = User("Jane")

    // An unbound member property reference
    // Pass a User receiver explicitly
    val unboundName = User::name
    println(unboundName.get(user))
    // Jane

    unboundName.set(user, "Jane Doe")
    println(user.name)
    // Jane Doe

    // A bound member property reference
    // Captures the user receiver
    val boundName = user::name

    println(boundName.get())
    // Jane Doe

    boundName.set("Jane Smith")
    println(user.name)
    // Jane Smith
}