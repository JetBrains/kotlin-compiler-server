fun interface Action {
    fun run()
}

fun execute(action: Action) {
    action.run()
}

fun start() {
    println("Run from a callable reference")
}

fun main() {
    // Kotlin converts the lambda to an Action instance
    execute { start() }
    // Run from a callable reference

    // Kotlin adapts the reference to an Action instance
    execute(::start)
    // Run from a callable reference
}