import kotlin.math.*

fun main() {
    val num = 27
    val negNum = -num

    println("The cube root of ${num.toDouble()} is: " + 
            cbrt(num.toDouble()))
    println("The cube root of ${negNum.toDouble()} is: " + 
            cbrt(negNum.toDouble()))
}