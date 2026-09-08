enum class ProtocolState {
    WAITING {
        override fun signal() = TALKING
    },

    TALKING {
        override fun signal() = WAITING
    };

    abstract fun signal(): ProtocolState
}

fun main() {
    var state = ProtocolState.WAITING

    println(state)
    // WAITING
    state = state.signal()
    println(state)
    // TALKING
}