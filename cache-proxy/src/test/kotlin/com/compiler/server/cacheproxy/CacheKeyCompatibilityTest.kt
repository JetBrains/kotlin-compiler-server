package com.compiler.server.cacheproxy

import com.compiler.server.api.ProjectFileRequestDto
import com.compiler.server.api.TranslateComposeWasmRequest
import com.compiler.server.cacheproxy.service.CacheService
import com.compiler.server.model.Project
import com.compiler.server.model.ProjectFile
import io.lettuce.core.api.sync.RedisCommands
import org.mockito.kotlin.mock
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class CacheKeyCompatibilityTest {

    private val cacheService = CacheService(
        redis = mock<RedisCommands<String, String>>(),
        kotlinVersion = "2.3.20",
        cacheNamespace = "prod",
        ttl = Duration.ofHours(24),
    )

    // --- V1 tests ---

    @Test
    fun `V1 key format matches expected pattern`() {
        val request = Project(
            args = "",
            files = listOf(ProjectFile(text = "fun main() {}", name = "Main.kt")),
        )
        val key = cacheService.buildKey(request)
        assert(key.startsWith("compose-wasm-v1:prod:v2.3.20:")) { "Key prefix mismatch: $key" }
        assert(key.substringAfterLast(":").length == 64) { "Hash should be 64 hex chars: $key" }
    }

    @Test
    fun `V1 file order does not affect key`() {
        val request1 = Project(
            files = listOf(
                ProjectFile(text = "class A", name = "A.kt"),
                ProjectFile(text = "class B", name = "B.kt"),
            ),
        )
        val request2 = Project(
            files = listOf(
                ProjectFile(text = "class B", name = "B.kt"),
                ProjectFile(text = "class A", name = "A.kt"),
            ),
        )
        assertEquals(cacheService.buildKey(request1), cacheService.buildKey(request2))
    }

    @Test
    fun `V1 different content produces different keys`() {
        val request1 = Project(files = listOf(ProjectFile(text = "fun a() {}", name = "Main.kt")))
        val request2 = Project(files = listOf(ProjectFile(text = "fun b() {}", name = "Main.kt")))
        assertNotEquals(cacheService.buildKey(request1), cacheService.buildKey(request2))
    }

    @Test
    fun `V1 compiler arguments are included in key`() {
        val base = Project(files = listOf(ProjectFile(text = "fun main() {}", name = "Main.kt")))
        val withArgs = base.copy(compilerArguments = listOf(mapOf("opt" to true)))
        assertNotEquals(cacheService.buildKey(base), cacheService.buildKey(withArgs))
    }

    // --- V2 tests ---

    @Test
    fun `V2 key format matches expected pattern`() {
        val request = TranslateComposeWasmRequest(
            args = "",
            files = listOf(ProjectFileRequestDto(text = "fun main() {}", name = "Main.kt")),
        )
        val key = cacheService.buildKey(request)
        assert(key.startsWith("compose-wasm-V2:prod:v2.3.20:")) { "Key prefix mismatch: $key" }
        assert(key.substringAfterLast(":").length == 64) { "Hash should be 64 hex chars: $key" }
    }

    @Test
    fun `V2 file order does not affect key`() {
        val request1 = TranslateComposeWasmRequest(
            files = listOf(
                ProjectFileRequestDto(text = "class A", name = "A.kt"),
                ProjectFileRequestDto(text = "class B", name = "B.kt"),
            ),
        )
        val request2 = TranslateComposeWasmRequest(
            files = listOf(
                ProjectFileRequestDto(text = "class B", name = "B.kt"),
                ProjectFileRequestDto(text = "class A", name = "A.kt"),
            ),
        )
        assertEquals(cacheService.buildKey(request1), cacheService.buildKey(request2))
    }

    @Test
    fun `V2 different content produces different keys`() {
        val request1 = TranslateComposeWasmRequest(files = listOf(ProjectFileRequestDto(text = "fun a() {}", name = "Main.kt")))
        val request2 = TranslateComposeWasmRequest(files = listOf(ProjectFileRequestDto(text = "fun b() {}", name = "Main.kt")))
        assertNotEquals(cacheService.buildKey(request1), cacheService.buildKey(request2))
    }

    @Test
    fun `V2 compiler arguments are included in key`() {
        val base = TranslateComposeWasmRequest(files = listOf(ProjectFileRequestDto(text = "fun main() {}", name = "Main.kt")))
        val withFirstPhase = TranslateComposeWasmRequest(
            args = base.args,
            files = base.files,
            firstPhaseCompilerArguments = mapOf("opt" to true),
            secondPhaseCompilerArguments = base.secondPhaseCompilerArguments,
        )
        val withSecondPhase = TranslateComposeWasmRequest(
            args = base.args,
            files = base.files,
            firstPhaseCompilerArguments = base.firstPhaseCompilerArguments,
            secondPhaseCompilerArguments = mapOf("opt" to true),
        )
        assertNotEquals(cacheService.buildKey(base), cacheService.buildKey(withFirstPhase))
        assertNotEquals(cacheService.buildKey(base), cacheService.buildKey(withSecondPhase))
        assertNotEquals(cacheService.buildKey(withFirstPhase), cacheService.buildKey(withSecondPhase))
    }

    @Test
    fun `V1 and V2 keys use different prefixes`() {
        val v1 = Project(files = listOf(ProjectFile(text = "fun main() {}", name = "Main.kt")))
        val v2 = TranslateComposeWasmRequest(files = listOf(ProjectFileRequestDto(text = "fun main() {}", name = "Main.kt")))
        val v1Key = cacheService.buildKey(v1)
        val v2Key = cacheService.buildKey(v2)
        assert(v1Key.startsWith("compose-wasm-v1:")) { "V1 key should start with compose-wasm-v1:" }
        assert(v2Key.startsWith("compose-wasm-V2:")) { "V2 key should start with compose-wasm-V2:" }
        assertNotEquals(v1Key, v2Key)
    }
}
