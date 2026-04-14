package com.compiler.server.cacheproxy

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.amazonaws.services.lambda.runtime.RequestStreamHandler
import com.compiler.server.cacheproxy.dto.ProjectRequest
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.api.sync.RedisCommands
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.lambda.LambdaClient
import software.amazon.awssdk.services.lambda.model.InvokeRequest
import java.security.MessageDigest
import java.time.Duration

class CacheProxyHandler : RequestStreamHandler {

    private val mapper = jacksonObjectMapper()
    private val idempotentMapper: JsonMapper = JsonMapper.builder()
        .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
        .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
        .build()

    private val targetLambda = System.getenv("TARGET_LAMBDA_NAME")
    private val kotlinVersion = System.getenv("KOTLIN_VERSION")
    private val cacheNamespace = System.getenv("CACHE_NAMESPACE") ?: "default"
    private val ttlSeconds = 86400L // 24 hours

    private val lambdaClient: LambdaClient = LambdaClient.create()
    private val redis: RedisCommands<String, String> = createRedisConnection()

    override fun handleRequest(input: java.io.InputStream, output: java.io.OutputStream, context: Context) {
        val log = context.logger
        val event = mapper.readTree(input)
        val path = event.get("path")?.asText() ?: ""
        val body = event.get("body")?.asText()
        val queryParams = event.get("queryStringParameters")
        val compiler = queryParams?.get("compiler")?.asText()
        log.log("cache-proxy: path=$path compiler=$compiler bodyLength=${body?.length ?: 0}\n")

        // Only cache compose-wasm requests; forward everything else directly
        if (compiler != "compose-wasm" || body == null) {
            log.log("cache-proxy: not cacheable, forwarding\n")
            val response = forward(event.toString(), log)
            output.write(response.toByteArray())
            return
        }

        // Parse request for cache key
        val request = try {
            mapper.readValue<ProjectRequest>(body)
        } catch (e: Exception) {
            log.log("cache-proxy: parse failed: ${e.message}\n")
            val response = forward(event.toString(), log)
            output.write(response.toByteArray())
            return
        }
        val key = buildCacheKey(request)
        log.log("cache-proxy: key=$key\n")

        // Check cache
        try {
            redis.get(key)?.let { cached ->
                redis.expire(key, ttlSeconds)
                log.log("cache-proxy: HIT (${cached.length} bytes)\n")
                val hit = mapper.writeValueAsString(mapOf(
                    "statusCode" to 200,
                    "headers" to mapOf("Content-Type" to "application/json"),
                    "body" to cached,
                    "isBase64Encoded" to false,
                ))
                output.write(hit.toByteArray())
                return
            }
            log.log("cache-proxy: MISS\n")
        } catch (e: Exception) {
            log.log("cache-proxy: Redis GET failed: ${e::class.simpleName}: ${e.message}\n")
        }

        // Forward to main Lambda
        val response = forward(event.toString(), log)
        val responseNode = mapper.readTree(response)
        val statusCode = responseNode.get("statusCode")?.asInt() ?: 500
        val responseBody = responseNode.get("body")?.asText()

        // Cache successful, error-free results
        if (statusCode == 200 && responseBody != null) {
            try {
                val errors = hasErrors(responseBody)
                log.log("cache-proxy: hasErrors=$errors responseLength=${responseBody.length}\n")
                if (!errors) {
                    redis.setex(key, ttlSeconds, responseBody)
                    log.log("cache-proxy: PUT OK\n")
                }
            } catch (e: Exception) {
                log.log("cache-proxy: Redis SET failed: ${e::class.simpleName}: ${e.message}\n")
            }
        } else {
            log.log("cache-proxy: not caching: statusCode=$statusCode hasBody=${responseBody != null}\n")
        }

        output.write(response.toByteArray())
    }

    private fun forward(eventJson: String, log: LambdaLogger): String {
        log.log("cache-proxy: invoking $targetLambda\n")
        val result = lambdaClient.invoke(
            InvokeRequest.builder()
                .functionName(targetLambda)
                .payload(SdkBytes.fromUtf8String(eventJson))
                .build()
        )
        val payload = result.payload().asUtf8String()
        if (result.functionError() != null) {
            log.log("cache-proxy: Lambda error: ${result.functionError()} payload=${payload.take(500)}\n")
        }
        val statusCode = mapper.readTree(payload).get("statusCode")?.asInt() ?: -1
        log.log("cache-proxy: Lambda returned statusCode=$statusCode\n")
        return payload
    }

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

    private fun hasErrors(body: String): Boolean {
        val node = mapper.readTree(body)
        if (node.get("exception")?.isNull == false) return true
        node.get("errors")?.fields()?.forEach { (_, diagnostics) ->
            diagnostics.forEach { if (it.get("severity")?.asText() == "ERROR") return true }
        }
        return false
    }

    private fun createRedisConnection(): RedisCommands<String, String> {
        val host = System.getenv("REDIS_HOST")
        val port = System.getenv("REDIS_PORT")?.toIntOrNull() ?: 6379
        val ssl = System.getenv("REDIS_SSL")?.toBoolean() ?: false
        println("cache-proxy init: Redis host=$host port=$port ssl=$ssl targetLambda=$targetLambda kotlinVersion=$kotlinVersion namespace=$cacheNamespace")
        val uri = RedisURI.builder()
            .withHost(host)
            .withPort(port)
            .withSsl(ssl)
            .withVerifyPeer(io.lettuce.core.SslVerifyMode.NONE)
            .withTimeout(Duration.ofSeconds(2))
            .build()
        return RedisClient.create(uri).connect().sync()
    }
}
