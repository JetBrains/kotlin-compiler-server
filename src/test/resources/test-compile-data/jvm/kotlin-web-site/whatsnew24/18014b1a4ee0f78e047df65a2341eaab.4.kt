data class Response(val body: String)

class Service {
    var queryCount = 0

    fun query(key: String): Response? {
        queryCount += 1
        return null
    }
}

//sampleStart
@OptIn(ExperimentalStdlibApi::class)
fun main() {
    val service = Service()
    val cache = mutableMapOf<String, Response?>()

    fun getCachedResponseOrQuery(key: String): Response? =
        cache.getOrPutIfMissing(key) { service.query(key) }

    // Stores null because the cache doesn't contain "user"
    getCachedResponseOrQuery("user")

    println(cache)
    // {user=null}

    // Uses the cached null and doesn't query the service again
    getCachedResponseOrQuery("user")

    println(service.queryCount)
    // 1
}
//sampleEnd