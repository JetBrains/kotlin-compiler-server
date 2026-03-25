package com.compiler.server.service

import com.compiler.server.api.TranslateComposeWasmRequest
import com.compiler.server.enums.CacheEndpointType
import com.compiler.server.model.Project
import com.compiler.server.model.TranslationWasmResult
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.time.Duration

@Service
@ConditionalOnProperty("wasm.compose.cache.enabled")
class WasmComposeCacheService(
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
    @Value("\${kotlin.version}") private val kotlinVersion: String,
    @Value("\${CACHE_NAMESPACE:default}") private val cacheNamespace: String,
    @Value("\${wasm.compose.cache.ttl-hours:24}") private val ttlHours: Long,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val idempotentMapper: ObjectMapper = JsonMapper.builder()
        .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
        .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
        .build()

    fun get(request: Any, endpointType: CacheEndpointType): TranslationWasmResult? {

        return try {
            val key = buildKey(request, endpointType)
            val result = redisTemplate.opsForValue().get(key)
                ?.let { objectMapper.readValue(it, TranslationWasmResult::class.java) }
                ?.also {
                    // Sliding TTL: reset expiry on every hit so frequently used entries never expire
                    redisTemplate.expire(key, Duration.ofHours(ttlHours))
                    log.debug("Cache HIT key={}", key)
                }
            result
        } catch (e: Exception) {
            log.warn("Cache GET failed, proceeding without cache. request=$request, exception=$endpointType", e)
            null
        }
    }

    fun put(request: Any, result: TranslationWasmResult, endpointType: CacheEndpointType) {
        try {
            val key = buildKey(request, endpointType)
            val json = objectMapper.writeValueAsString(result)
            redisTemplate.opsForValue().set(key, json, Duration.ofHours(ttlHours))
            log.debug("Cache PUT key={}", key)
        } catch (e: Exception) {
            log.warn("Cache PUT failed, result not cached. request=$request, endpointType=$endpointType", e)
        }
    }

    private fun buildKey(request: Any, cacheEndpointType: CacheEndpointType): String {
        val normalized = when {
            CacheEndpointType.COMPOSE_WASM_V2 == cacheEndpointType && request is TranslateComposeWasmRequest -> {
                mapOf(
                    "args" to request.args,
                    "files" to request.files.sortedBy { it.name },
                    "firstPhaseCompilerArguments" to request.firstPhaseCompilerArguments,
                    "secondPhaseCompilerArguments" to request.secondPhaseCompilerArguments,
                )
            }

            CacheEndpointType.COMPOSE_WASM_V1 == cacheEndpointType && request is Project -> {
                mapOf(
                    "args" to request.args,
                    "files" to request.files.sortedBy { it.name },
                    "compilerArguments" to request.compilerArguments,
                )
            }

            else -> throw IllegalArgumentException(
                "Unsupported cache endpoint type $cacheEndpointType" +
                        " for given request type ${request::class.java}"
            )
        }
        val requestString = idempotentMapper.writeValueAsString(normalized)
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(requestString.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return "${cacheEndpointType.cacheName}:$cacheNamespace:v$kotlinVersion:$hash"
    }
}
