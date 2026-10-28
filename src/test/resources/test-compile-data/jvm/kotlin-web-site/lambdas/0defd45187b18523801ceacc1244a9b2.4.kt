fun isOdd(number: Int) = number % 2 != 0

fun main() {
    val numbers = listOf(1, 2, 3, 4, 5)

    // Pass a reference to isOdd()
    println(numbers.filter(::isOdd))
    // [1, 3, 5]
}