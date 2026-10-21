interface Base {
    val message: String
    fun print()
}

class BaseImpl(x: Int) : Base {
    override val message = "BaseImpl: x = $x"
    override fun print() { println(message) }
}

class Derived(b: Base) : Base by b {
    // This property is not accessible
    // from b's implementation of `print()`
    override val message = "Message of Derived"
}

fun main() {
    val base = BaseImpl(10)
    val derived = Derived(base)
    
    derived.print()
    // BaseImpl: x = 10
    println(derived.message)
    // Message of Derived
}