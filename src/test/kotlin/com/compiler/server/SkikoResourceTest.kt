package com.compiler.server

import com.compiler.server.base.BaseExecutorTest
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
class SkikoResourceTest : BaseExecutorTest() {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Value("\${kotlin.version}")
    private lateinit var kotlinVersion: String

    @Test
    fun `test non caching for skiko mjs resource`() {
        val resourceUrl = "/api/resource/skiko.mjs" // Ensure this matches your resource path

        mockMvc
            .perform(MockMvcRequestBuilders.get(resourceUrl))
            .andExpect(MockMvcResultMatchers.status().isOk) // Ensures HTTP status 200
            .andExpect(
                MockMvcResultMatchers.header().doesNotExist(HttpHeaders.CACHE_CONTROL)
            )
            .andExpect(
                MockMvcResultMatchers.header().string(
                    HttpHeaders.CONTENT_TYPE,
                    "text/javascript"
                )
            )
    }

    @Test
    fun `test caching headers for skiko mjs resource`() {
        val resourceUrl = "/api/resource/skiko-$kotlinVersion.mjs" // Ensure this matches your resource path
        val expectedCacheControl = "max-age=${TimeUnit.DAYS.toSeconds(365)}"

        mockMvc
            .perform(MockMvcRequestBuilders.get(resourceUrl))
            .andExpect(MockMvcResultMatchers.status().isOk) // Ensures HTTP status 200
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
                    "text/javascript"
                )
            )
    }

    @Test
    fun `test caching headers for skiko wasm resource`() {
        val resourceUrl = "/api/resource/skiko-$kotlinVersion.wasm"
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
                    "application/wasm"
                )
            )
    }

    @Test
    fun `test non caching for skiko wasm resource`() {
        val resourceUrl = "/api/resource/skiko.wasm"

        mockMvc
            .perform(MockMvcRequestBuilders.get(resourceUrl))
            .andExpect(MockMvcResultMatchers.status().isOk) // HTTP 200 status
            .andExpect(
                MockMvcResultMatchers.header().doesNotExist(HttpHeaders.CACHE_CONTROL)
            )
            .andExpect(
                MockMvcResultMatchers.header().string(
                    HttpHeaders.CONTENT_TYPE,
                    "application/wasm"
                )
            )
    }
}