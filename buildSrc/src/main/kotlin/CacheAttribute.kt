import org.gradle.api.attributes.Attribute

enum class CacheAttribute {
    STDLIB;

    companion object {
        val cacheAttribute = Attribute.of("org.jetbrains.kotlin-compiler-server.cache", CacheAttribute::class.java)
    }
}