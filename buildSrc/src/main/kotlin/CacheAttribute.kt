import org.gradle.api.attributes.Attribute

enum class CacheAttribute {
    LOCAL,
    LAMBDA;

    companion object {
        val cacheAttribute = Attribute.of("org.jetbrains.kotlin-compiler-server.cache", CacheAttribute::class.java)
    }
}