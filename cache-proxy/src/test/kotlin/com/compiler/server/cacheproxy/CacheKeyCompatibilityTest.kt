package com.compiler.server.cacheproxy

import com.compiler.server.cacheproxy.dto.FileDto
import com.compiler.server.cacheproxy.dto.ProjectRequest
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class CacheKeyCompatibilityTest {

    private val kotlinVersion = "2.3.20"
    private val cacheNamespace = "prod"

    private val idempotentMapper = JsonMapper.builder()
        .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
        .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
        .build()

    private fun buildCacheKey(request: ProjectRequest): String {
        val normalized = mapOf(
            "args" to request.args,
            "files" to request.files.sortedBy { it.name },
            "compilerArguments" to request.compilerArguments,
        )
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(idempotentMapper.writeValueAsString(normalized).toByteArray())
            .joinToString("") { "%02x".format(it) }
        return "compose-wasm-v1:$cacheNamespace:v$kotlinVersion:$hash"
    }

    @Test
    fun `key format matches expected pattern`() {
        val request = ProjectRequest(
            args = "",
            files = listOf(FileDto(text = "fun main() {}", name = "Main.kt")),
        )
        val key = buildCacheKey(request)
        assert(key.startsWith("compose-wasm-v1:prod:v2.3.20:")) { "Key prefix mismatch: $key" }
        assert(key.substringAfterLast(":").length == 64) { "Hash should be 64 hex chars: $key" }
    }

    @Test
    fun `file order does not affect key`() {
        val request1 = ProjectRequest(
            files = listOf(
                FileDto(text = "class A", name = "A.kt"),
                FileDto(text = "class B", name = "B.kt"),
            ),
        )
        val request2 = ProjectRequest(
            files = listOf(
                FileDto(text = "class B", name = "B.kt"),
                FileDto(text = "class A", name = "A.kt"),
            ),
        )
        assertEquals(buildCacheKey(request1), buildCacheKey(request2))
    }

    @Test
    fun `different content produces different keys`() {
        val request1 = ProjectRequest(files = listOf(FileDto(text = "fun a() {}", name = "Main.kt")))
        val request2 = ProjectRequest(files = listOf(FileDto(text = "fun b() {}", name = "Main.kt")))
        assertNotEquals(buildCacheKey(request1), buildCacheKey(request2))
    }

    @Test
    fun `compiler arguments are included in key`() {
        val base = ProjectRequest(files = listOf(FileDto(text = "fun main() {}", name = "Main.kt")))
        val withArgs = base.copy(compilerArguments = listOf(mapOf("opt" to true)))
        assertNotEquals(buildCacheKey(base), buildCacheKey(withArgs))
    }
}
