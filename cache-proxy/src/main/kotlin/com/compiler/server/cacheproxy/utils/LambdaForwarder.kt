package com.compiler.server.cacheproxy.utils

import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.fasterxml.jackson.databind.ObjectMapper
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.lambda.LambdaClient
import software.amazon.awssdk.services.lambda.model.InvokeRequest

fun LambdaClient.forwardEvent(
    eventJson: String,
    targetLambda: String,
    mapper: ObjectMapper,
    log: LambdaLogger,
): String {
    log.log("cache-proxy: invoking $targetLambda\n")
    val result = invoke(
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
