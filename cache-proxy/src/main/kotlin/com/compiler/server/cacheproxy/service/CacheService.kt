package com.compiler.server.cacheproxy.service

import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.compiler.server.api.CacheableRequest
import com.compiler.server.api.TranslateComposeWasmRequest
import com.compiler.server.cacheproxy.enums.CacheEndpointType
import com.compiler.server.model.Project
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import io.lettuce.core.RedisConnectionException
import io.lettuce.core.api.sync.RedisCommands
import java.security.MessageDigest
import java.time.Duration

class CacheService(
    private val redis: RedisCommands<String, String>,
    private val cacheNamespace: String,
    private val ttl: Duration,
) {
    private val idempotentMapper: JsonMapper = JsonMapper.builder()
        .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
        .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
        .build()

    fun get(request: CacheableRequest, kotlinVersion: String, log: LambdaLogger): String? {
        val key = buildKey(request, kotlinVersion)
        return withRetryOnConnectionLoss("GET", log) {
            redis.get(key)?.also {
                redis.expire(key, ttl.seconds)
                log.log("cache-proxy: HIT key=$key (${it.length} bytes)\n")
            }
        }
    }

    fun put(request: CacheableRequest, kotlinVersion: String, responseBody: String, log: LambdaLogger) {
        val key = buildKey(request, kotlinVersion)
        withRetryOnConnectionLoss("SET", log) {
            redis.setex(key, ttl.seconds, responseBody)
            log.log("cache-proxy: PUT OK key=$key\n")
        }
    }

    // The first command after a Lambda freeze tends to fail because the socket was
    // silently dropped (NLB idle timeout) while the container was paused. Lettuce
    // schedules an async reconnect on that failure; retrying once lets Lettuce's
    // internal queue dispatch the command on the fresh connection.
    private inline fun <T> withRetryOnConnectionLoss(op: String, log: LambdaLogger, block: () -> T): T? =
        try {
            block()
        } catch (e: RedisConnectionException) {
            log.log("cache-proxy: Redis $op connection lost, retrying once: ${e.message}\n")
            try {
                block()
            } catch (e2: Exception) {
                log.log("cache-proxy: Redis $op retry failed: ${e2::class.simpleName}: ${e2.message}\n")
                null
            }
        } catch (e: Exception) {
            log.log("cache-proxy: Redis $op failed: ${e::class.simpleName}: ${e.message}\n")
            null
        }

    internal fun buildKey(request: CacheableRequest, kotlinVersion: String): String {
        val (endpointType, normalized) = when (request) {
            is TranslateComposeWasmRequest -> CacheEndpointType.COMPOSE_WASM_V2 to mapOf(
                "args" to request.args,
                "files" to request.files.sortedBy { it.name },
                "firstPhaseCompilerArguments" to request.firstPhaseCompilerArguments,
                "secondPhaseCompilerArguments" to request.secondPhaseCompilerArguments,
            )
            is Project -> CacheEndpointType.COMPOSE_WASM_V1 to mapOf(
                "args" to request.args,
                "files" to request.files.sortedBy { it.name },
                "compilerArguments" to request.compilerArguments,
            )
            else -> error("Unsupported cacheable request type: ${request::class.simpleName}")
        }
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(idempotentMapper.writeValueAsString(normalized).toByteArray())
            .joinToString("") { "%02x".format(it) }
        return "${endpointType.cacheName}:$cacheNamespace:v$kotlinVersion:$hash"
    }
}
