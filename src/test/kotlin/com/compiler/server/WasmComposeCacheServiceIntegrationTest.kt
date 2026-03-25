package com.compiler.server

import com.compiler.server.api.ProjectFileRequestDto
import com.compiler.server.api.TranslateComposeWasmRequest
import com.compiler.server.model.Project
import com.compiler.server.service.KotlinProjectExecutor
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

@SpringBootTest
@AutoConfigureMockMvc
class WasmComposeCacheServiceIntegrationTest {

    companion object {
        private val redis = GenericContainer(DockerImageName.parse("valkey/valkey:9.1"))
            .withExposedPorts(6379)

        init {
            redis.start()
        }

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.data.redis.host") { redis.host }
            registry.add("spring.data.redis.port") { redis.getMappedPort(6379) }
            registry.add("wasm.compose.cache.enabled") { "true" }
        }
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoSpyBean
    private lateinit var kotlinProjectExecutor: KotlinProjectExecutor

    @Test
    fun `test cache hit and miss via controller`() {
        val request = TranslateComposeWasmRequest(
            files = listOf(ProjectFileRequestDto("fun main() { println(\"hello\") }", "Main.kt")),
            args = ""
        )

        // First call - Cache Miss
        mockMvc.post("/api/compiler/translate/compose-wasm") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isOk() }
        }

        // Verify it was called once on the executor
        verify(kotlinProjectExecutor, times(1)).convertToWasm(any<Project>(), anyBoolean())

        // Second call - Cache Hit
        mockMvc.post("/api/compiler/translate/compose-wasm") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isOk() }
        }

        // Verify it was STILL called ONLY once on the executor (second call was a cache hit)
        verify(kotlinProjectExecutor, times(1)).convertToWasm(any<Project>(), anyBoolean())
    }
}
