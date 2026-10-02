fun calculateOrderTotals(prices: List<Double>): Pair<Double, Double> {
    val subtotal = prices.sum()
    val tax = subtotal * 0.2
    return Pair(subtotal, tax)
}

fun main() {
    val totals = calculateOrderTotals(listOf(12.50, 8.00, 4.50))

    // What does 'first' mean?
    println(totals.first)
    // 25.0
  
    // What does 'second' mean?
    println(totals.second)
    // 5.0
}