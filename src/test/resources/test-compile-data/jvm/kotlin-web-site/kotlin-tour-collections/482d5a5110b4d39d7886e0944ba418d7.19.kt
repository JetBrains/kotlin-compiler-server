val greenNumbers = listOf(1, 4, 23)
val redNumbers = listOf(17, 2)

fun main() {
    val totalCount = greenNumbers.count() + redNumbers.count()
    println(totalCount)
}