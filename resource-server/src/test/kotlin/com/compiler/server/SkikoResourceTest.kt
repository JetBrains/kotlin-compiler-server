package com.compiler.server

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.util.concurrent.TimeUnit

@SpringBootTest
@AutoConfigureMockMvc
class SkikoResourceTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Value("\${skiko.version}")
    private lateinit var skikoVersion: String

    @Value("\${dependencies.compose.wasm}")
    private lateinit var stdlibHash: String

    @Test
    fun `test caching headers for skiko mjs resource`() {
        testCachingHeadersForResource(
            "/api/resource/skiko-$skikoVersion.mjs",
            "text/javascript"
        )
    }

    @Test
    fun `test caching headers for skiko wasm resource`() {
        testCachingHeadersForResource(
            "/api/resource/skiko-$skikoVersion.wasm",
            "application/wasm"
        )
    }

    @Test
    fun `test caching headers for stdlib mjs resource`() {
        testCachingHeadersForResource(
            "/api/resource/stdlib-$stdlibHash.mjs",
            "text/javascript"
        )
    }

    @Test
    fun `test caching headers for stdlib wasm resource`() {
        testCachingHeadersForResource(
            "/api/resource/stdlib-$stdlibHash.wasm",
            "application/wasm"
        )
    }

    private fun testCachingHeadersForResource(
        resourceUrl: String,
        contentType: String
    ) {
        val expectedCacheControl = "max-age=${TimeUnit.DAYS.toSeconds(365)}"

        mockMvc
            .perform(MockMvcRequestBuilders.get(resourceUrl))
            .andExpect(MockMvcResultMatchers.status().isOk) // HTTP 200 status
            .andExpect(
                MockMvcResultMatchers.header().exists(HttpHeaders.CACHE_CONTROL)
            )
            .andExpect(
                MockMvcResultMatchers.header().string(
                    HttpHeaders.CACHE_CONTROL,
                    expectedCacheControl
                )
            )
            .andExpect(
                MockMvcResultMatchers.header().string(
                    HttpHeaders.CONTENT_TYPE,
                    contentType
                )
            )
    }
}