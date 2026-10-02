data class OrderSummary(
    val subtotal: Double,
    val tax: Double,
)

fun calculateOrderSummary(prices: List<Double>): OrderSummary {
    val subtotal = prices.sum()
    val tax = subtotal * 0.2
    return OrderSummary(subtotal, tax)
}

fun main() {
    val summary = calculateOrderSummary(listOf(12.50, 8.00, 4.50))

    println(summary.subtotal)
    // 25.0
    println(summary.tax)
    // 5.0
}