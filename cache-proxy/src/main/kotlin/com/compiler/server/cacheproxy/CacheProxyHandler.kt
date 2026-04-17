package com.compiler.server.cacheproxy

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.amazonaws.services.lambda.runtime.RequestStreamHandler
import com.compiler.server.cacheproxy.service.CacheService
import com.compiler.server.cacheproxy.service.createRedisConnectionFromEnv
import com.compiler.server.cacheproxy.service.forwardEvent
import com.compiler.server.cacheproxy.service.parseComposeRequest
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import software.amazon.awssdk.services.lambda.LambdaClient
import java.io.InputStream
import java.io.OutputStream
import java.time.Duration

class CacheProxyHandler : RequestStreamHandler {

    private val mapper = jacksonObjectMapper()

    private val cacheService = CacheService(
        redis = createRedisConnectionFromEnv(),
        kotlinVersion = System.getenv("KOTLIN_VERSION"),
        cacheNamespace = System.getenv("CACHE_NAMESPACE") ?: "default",
        ttl = Duration.ofHours(24),
    )
    private val lambdaClient: LambdaClient = LambdaClient.create()
    private val targetLambda: String = System.getenv("TARGET_LAMBDA_NAME")

    override fun handleRequest(input: InputStream, output: OutputStream, context: Context) {
        val log = context.logger
        val event = mapper.readTree(input)
        val request = parseComposeRequest(event, mapper, log)

        if (request == null) {
            log.log("cache-proxy: not cacheable, forwarding\n")
            val response = lambdaClient.forwardEvent(event.toString(), targetLambda, mapper, log)
            output.write(response.toByteArray())
            return
        }

        cacheService.get(request, log)?.let { cached ->
            output.write(buildHitResponse(cached).toByteArray())
            return
        }
        log.log("cache-proxy: MISS, forwarding\n")

        val response = lambdaClient.forwardEvent(event.toString(), targetLambda, mapper, log)
        extractBodyToCache(response, log)?.let { body -> cacheService.put(request, body, log) }
        output.write(response.toByteArray())
    }

    private fun buildHitResponse(body: String): String = mapper.writeValueAsString(
        mapOf(
            "statusCode" to 200,
            "headers" to mapOf("Content-Type" to "application/json"),
            "body" to body,
            "isBase64Encoded" to false,
        )
    )

    private fun extractBodyToCache(response: String, log: LambdaLogger): String? {
        val node = mapper.readTree(response)
        val statusCode = node.get("statusCode")?.asInt() ?: 500
        val body = node.get("body")?.asText()
        if (statusCode != 200 || body == null) {
            log.log("cache-proxy: not caching: statusCode=$statusCode hasBody=${body != null}\n")
            return null
        }
        if (hasErrors(body)) {
            log.log("cache-proxy: not caching: response contains errors\n")
            return null
        }
        return body
    }

    private fun hasErrors(body: String): Boolean {
        val node = mapper.readTree(body)
        if (node.get("exception")?.isNull == false) return true
        node.get("errors")?.properties()?.forEach { (_, diagnostics) ->
            diagnostics.forEach { if (it.get("severity")?.asText() == "ERROR") return true }
        }
        return false
    }
}
