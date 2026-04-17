package com.compiler.server.cacheproxy.service

import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.compiler.server.api.CacheableRequest
import com.compiler.server.api.TranslateComposeWasmRequest
import com.compiler.server.model.Project
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

fun parseComposeRequest(event: JsonNode, mapper: ObjectMapper, log: LambdaLogger): CacheableRequest? {
    val path = event.get("path")?.asText() ?: ""
    val body = event.get("body")?.asText() ?: return null
    val compiler = event.get("queryStringParameters")?.get("compiler")?.asText()

    return when {
        path.endsWith("/translate/compose-wasm") -> tryParse<TranslateComposeWasmRequest>(mapper, body, "V2", log)
        path.endsWith("/translate") && compiler == "compose-wasm" -> tryParse<Project>(mapper, body, "V1", log)
        else -> null
    }
}

private inline fun <reified T> tryParse(mapper: ObjectMapper, body: String, label: String, log: LambdaLogger): T? =
    try {
        mapper.readValue<T>(body)
    } catch (e: Exception) {
        log.log("cache-proxy: $label parse failed: ${e.message}\n")
        null
    }
