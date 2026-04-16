package com.compiler.server.cacheproxy

import com.compiler.server.cacheproxy.dto.ComposeWasmV2Request
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

    private fun buildV1CacheKey(request: ProjectRequest): String {
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

    private fun buildV2CacheKey(request: ComposeWasmV2Request): String {
        val normalized = mapOf(
            "args" to request.args,
            "files" to request.files.sortedBy { it.name },
            "firstPhaseCompilerArguments" to request.firstPhaseCompilerArguments,
            "secondPhaseCompilerArguments" to request.secondPhaseCompilerArguments,
        )
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(idempotentMapper.writeValueAsString(normalized).toByteArray())
            .joinToString("") { "%02x".format(it) }
        return "compose-wasm-V2:$cacheNamespace:v$kotlinVersion:$hash"
    }

    // --- V1 tests ---

    @Test
    fun `V1 key format matches expected pattern`() {
        val request = ProjectRequest(
            args = "",
            files = listOf(FileDto(text = "fun main() {}", name = "Main.kt")),
        )
        val key = buildV1CacheKey(request)
        assert(key.startsWith("compose-wasm-v1:prod:v2.3.20:")) { "Key prefix mismatch: $key" }
        assert(key.substringAfterLast(":").length == 64) { "Hash should be 64 hex chars: $key" }
    }

    @Test
    fun `V1 file order does not affect key`() {
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
        assertEquals(buildV1CacheKey(request1), buildV1CacheKey(request2))
    }

    @Test
    fun `V1 different content produces different keys`() {
        val request1 = ProjectRequest(files = listOf(FileDto(text = "fun a() {}", name = "Main.kt")))
        val request2 = ProjectRequest(files = listOf(FileDto(text = "fun b() {}", name = "Main.kt")))
        assertNotEquals(buildV1CacheKey(request1), buildV1CacheKey(request2))
    }

    @Test
    fun `V1 compiler arguments are included in key`() {
        val base = ProjectRequest(files = listOf(FileDto(text = "fun main() {}", name = "Main.kt")))
        val withArgs = base.copy(compilerArguments = listOf(mapOf("opt" to true)))
        assertNotEquals(buildV1CacheKey(base), buildV1CacheKey(withArgs))
    }

    // --- V2 tests ---

    @Test
    fun `V2 key format matches expected pattern`() {
        val request = ComposeWasmV2Request(
            args = "",
            files = listOf(FileDto(text = "fun main() {}", name = "Main.kt")),
        )
        val key = buildV2CacheKey(request)
        assert(key.startsWith("compose-wasm-V2:prod:v2.3.20:")) { "Key prefix mismatch: $key" }
        assert(key.substringAfterLast(":").length == 64) { "Hash should be 64 hex chars: $key" }
    }

    @Test
    fun `V2 file order does not affect key`() {
        val request1 = ComposeWasmV2Request(
            files = listOf(
                FileDto(text = "class A", name = "A.kt"),
                FileDto(text = "class B", name = "B.kt"),
            ),
        )
        val request2 = ComposeWasmV2Request(
            files = listOf(
                FileDto(text = "class B", name = "B.kt"),
                FileDto(text = "class A", name = "A.kt"),
            ),
        )
        assertEquals(buildV2CacheKey(request1), buildV2CacheKey(request2))
    }

    @Test
    fun `V2 different content produces different keys`() {
        val request1 = ComposeWasmV2Request(files = listOf(FileDto(text = "fun a() {}", name = "Main.kt")))
        val request2 = ComposeWasmV2Request(files = listOf(FileDto(text = "fun b() {}", name = "Main.kt")))
        assertNotEquals(buildV2CacheKey(request1), buildV2CacheKey(request2))
    }

    @Test
    fun `V2 compiler arguments are included in key`() {
        val base = ComposeWasmV2Request(files = listOf(FileDto(text = "fun main() {}", name = "Main.kt")))
        val withFirstPhase = base.copy(firstPhaseCompilerArguments = mapOf("opt" to true))
        val withSecondPhase = base.copy(secondPhaseCompilerArguments = mapOf("opt" to true))
        assertNotEquals(buildV2CacheKey(base), buildV2CacheKey(withFirstPhase))
        assertNotEquals(buildV2CacheKey(base), buildV2CacheKey(withSecondPhase))
        assertNotEquals(buildV2CacheKey(withFirstPhase), buildV2CacheKey(withSecondPhase))
    }

    @Test
    fun `V1 and V2 keys use different prefixes`() {
        val v1 = ProjectRequest(files = listOf(FileDto(text = "fun main() {}", name = "Main.kt")))
        val v2 = ComposeWasmV2Request(files = listOf(FileDto(text = "fun main() {}", name = "Main.kt")))
        val v1Key = buildV1CacheKey(v1)
        val v2Key = buildV2CacheKey(v2)
        assert(v1Key.startsWith("compose-wasm-v1:")) { "V1 key should start with compose-wasm-v1:" }
        assert(v2Key.startsWith("compose-wasm-V2:")) { "V2 key should start with compose-wasm-V2:" }
        assertNotEquals(v1Key, v2Key)
    }
}
