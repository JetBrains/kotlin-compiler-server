package com.compiler.server.cacheproxy

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.amazonaws.services.lambda.runtime.RequestStreamHandler
import com.compiler.server.cacheproxy.service.CacheService
import com.compiler.server.cacheproxy.utils.createRedisConnectionFromEnv
import com.compiler.server.cacheproxy.utils.extractKotlinVersion
import com.compiler.server.cacheproxy.utils.forwardEvent
import com.compiler.server.cacheproxy.utils.parseCacheableBody
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import software.amazon.awssdk.http.HttpStatusCode
import software.amazon.awssdk.services.lambda.LambdaClient
import java.io.InputStream
import java.io.OutputStream
import java.time.Duration

// @JvmOverloads generates a no-arg constructor using default parameter values, which is required for Lambda
class CacheProxyHandler @JvmOverloads constructor(
    private val cacheService: CacheService = CacheService(
        redis = createRedisConnectionFromEnv(),
        cacheNamespace = System.getenv("CACHE_NAMESPACE") ?: "default",
        ttl = Duration.ofHours(24),
    ),
    private val lambdaClient: LambdaClient = LambdaClient.create(),
    private val targetLambdas: Map<String, String> =
        jacksonObjectMapper().readValue(System.getenv("TARGET_LAMBDAS")),
    private val mapper: ObjectMapper = jacksonObjectMapper(),
) : RequestStreamHandler {

    override fun handleRequest(input: InputStream, output: OutputStream, context: Context) {
        val log = context.logger
        val event = mapper.readTree(input)

        val kotlinVersion = extractKotlinVersion(event)
        if (kotlinVersion == null) {
            log.log("cache-proxy: cannot extract version from path, returning 500\n")
            output.write(buildErrorResponse("Path does not match /api/{version}/compose/").toByteArray())
            return
        }
        val targetLambda = targetLambdas[kotlinVersion]
        if (targetLambda == null) {
            log.log("cache-proxy: no target lambda configured for version=$kotlinVersion, returning 500\n")
            output.write(buildErrorResponse("No target lambda for version $kotlinVersion").toByteArray())
            return
        }

        val request = parseCacheableBody(event, mapper, log)
        if (request == null) {
            log.log("cache-proxy: not cacheable, forwarding\n")
            val response = lambdaClient.forwardEvent(event.toString(), targetLambda, mapper, log)
            output.write(response.toByteArray())
            return
        }

        cacheService.get(request, kotlinVersion, log)?.let { cached ->
            output.write(buildHitResponse(cached).toByteArray())
            return
        }
        log.log("cache-proxy: MISS, forwarding\n")

        val response = lambdaClient.forwardEvent(event.toString(), targetLambda, mapper, log)
        extractBodyToCache(response, log)?.let { body -> cacheService.put(request, kotlinVersion, body, log) }
        output.write(response.toByteArray())
    }

    private fun buildHitResponse(body: String): String = mapper.writeValueAsString(
        mapOf(
            "statusCode" to HttpStatusCode.OK,
            "headers" to mapOf("Content-Type" to "application/json"),
            "body" to body,
            "isBase64Encoded" to false,
        )
    )

    private fun buildErrorResponse(message: String): String = mapper.writeValueAsString(
        mapOf(
            "statusCode" to HttpStatusCode.INTERNAL_SERVER_ERROR,
            "headers" to mapOf("Content-Type" to "application/json"),
            "body" to mapper.writeValueAsString(mapOf("error" to message)),
            "isBase64Encoded" to false,
        )
    )

    private fun extractBodyToCache(response: String, log: LambdaLogger): String? {
        val node = mapper.readTree(response)
        val statusCode = node.get("statusCode")?.asInt() ?: HttpStatusCode.INTERNAL_SERVER_ERROR
        val body = node.get("body")?.asText()
        if (statusCode != HttpStatusCode.OK || body == null) {
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
