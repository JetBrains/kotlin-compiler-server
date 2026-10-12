class User(val name: String) {
    fun hasName(prefix: String) = name.startsWith(prefix)
}

fun main() {
    // The first parameter of an unbound reference is the User receiver
    val unbound: (User, String) -> Boolean = User::hasName
    println(unbound(User("Jane"), "Ja"))
    // true

    val jane = User("Jane")

    // Reference captures jane as its receiver
    val bound: (String) -> Boolean = jane::hasName
    println(bound("Doe"))
    // false
}