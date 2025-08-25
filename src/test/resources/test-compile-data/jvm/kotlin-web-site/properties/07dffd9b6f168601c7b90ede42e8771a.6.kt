class ShoppingCart {
    // Backing property
    private val _items = mutableListOf<String>()

    // Public read-only view
    val items: List<String>
        get() = _items

    fun addItem(item: String) {
        _items.add(item)
    }

    fun removeItem(item: String) {
        _items.remove(item)
    }
}

fun main() {
    val cart = ShoppingCart()
    cart.addItem("Apple")
    cart.addItem("Banana")

    println(cart.items) 
    // [Apple, Banana]
    
    cart.removeItem("Apple")
    println(cart.items) 
    // [Banana]
}