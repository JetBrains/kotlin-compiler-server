interface UserPrinter {
    fun print()
}

fun main() {
    val printer = object : UserPrinter {
        val prefix = "User"
        
        override fun print() {
            // `this` refers to the anonymous object
            // `this.prefix` accesses its `prefix` property
            println(this.prefix)
        }
    }
    printer.print()
    // User
}