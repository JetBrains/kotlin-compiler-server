import org.gradle.api.attributes.Attribute

enum class CacheAttribute {
    FULL,
    WASM;

    companion object {
        val cacheAttribute = Attribute.of("org.jetbrains.kotlin-compiler-server.cache", CacheAttribute::class.java)
    }
}