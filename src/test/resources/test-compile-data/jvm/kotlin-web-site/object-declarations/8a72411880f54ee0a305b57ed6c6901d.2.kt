object MyObject {
    override fun toString(): String {
        val superString = super.toString()
        // MyObject@hashcode
        return superString.substringBefore('@')
    }
}

fun main() {
    println(MyObject)
    // MyObject
}