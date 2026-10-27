fun buildUrls(actions: List<String>, prefix: String, id: Int): List<String> {
    return actions.map { action -> "$prefix/$id/$action" }
}

fun main() {
    val actions = listOf("title", "year", "author")
    val urls = buildUrls(actions, "https://example.com/book-info", 5)
    println(urls)
}