// Declares an object nested inside another object declaration
object Outer {
    val name = "Outer"

    object Nested {
        val name = "Nested in Outer"
    }
}

// Declares an object nested inside a class
class Server {
    object Config {
        const val port = 8080
    }
}

fun main() {
    // To access a nested object, refer to it
    // using the name of its enclosing object
    println(Outer.Nested.name)
    // Nested in Outer

    println(Server.Config.port)
    // 8080

    // An object expression can be nested inside a function
    val local = object {
        val name = "Nested in main()"
    }
    println(local.name)
    // Nested in main()

    // An object declaration can't be nested inside a function
    // object Local {
    //     val name = "Nested in main()"
    // }
}