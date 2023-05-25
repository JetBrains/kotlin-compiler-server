fun lengthString(maybeString: String?): Int? = maybeString?.length

fun main() { 
    var nullString: String? = null
    println(lengthString(nullString))
    // null
}