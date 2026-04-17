package com.compiler.server.cacheproxy

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.compiler.server.cacheproxy.service.CacheService
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.lambda.LambdaClient
import software.amazon.awssdk.services.lambda.model.InvokeRequest
import software.amazon.awssdk.services.lambda.model.InvokeResponse
import java.io.ByteArrayOutputStream
import java.time.Duration
import kotlin.test.Test

class CacheProxyHandlerIntegrationTest {

    companion object {
        private val valkey = GenericContainer(DockerImageName.parse("valkey/valkey:9.1"))
            .withExposedPorts(6379)

        init {
            valkey.start()
        }
    }

    private val mapper = jacksonObjectMapper()

    private val redis = RedisClient.create(
        RedisURI.builder()
            .withHost(valkey.host)
            .withPort(valkey.getMappedPort(6379))
            .build()
    ).connect().sync()

    private val cacheService = CacheService(
        redis = redis,
        kotlinVersion = "2.3.20",
        cacheNamespace = "integ",
        ttl = Duration.ofHours(24),
    )

    private val lambdaClient = mock<LambdaClient>()

    private val handler = CacheProxyHandler(
        cacheService = cacheService,
        lambdaClient = lambdaClient,
        targetLambda = "test-target-lambda",
        mapper = mapper,
    )

    private val testLogger = object : LambdaLogger {
        override fun log(message: String) { print(message) }
        override fun log(message: ByteArray) { print(String(message)) }
    }
    private val testContext = mock<Context>().also {
        whenever(it.logger).thenReturn(testLogger)
    }

    @Test
    fun `second call with same V2 request is served from cache`() {
        val requestBody = mapper.writeValueAsString(
            mapOf(
                "args" to "",
                "files" to listOf(mapOf("text" to "fun main() {}", "name" to "Main.kt")),
            )
        )
        val event = mapper.writeValueAsString(
            mapOf(
                "path" to "/translate/compose-wasm",
                "body" to requestBody,
            )
        )
        val cannedBody = mapper.writeValueAsString(mapOf("jsCode" to "console.log('hi')"))
        val cannedResponse = mapper.writeValueAsString(
            mapOf("statusCode" to 200, "body" to cannedBody)
        )

        whenever(lambdaClient.invoke(any<InvokeRequest>())).thenReturn(
            InvokeResponse.builder().payload(SdkBytes.fromUtf8String(cannedResponse)).build()
        )

        // First call — cache MISS, forwards to Lambda
        handler.handleRequest(event.byteInputStream(), ByteArrayOutputStream(), testContext)

        // Second call — cache HIT, must not forward again
        handler.handleRequest(event.byteInputStream(), ByteArrayOutputStream(), testContext)

        verify(lambdaClient, times(1)).invoke(any<InvokeRequest>())
    }
}
