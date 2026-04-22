package com.compiler.server.cacheproxy.utils

import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.SslVerifyMode
import io.lettuce.core.api.sync.RedisCommands
import java.time.Duration

fun createRedisConnectionFromEnv(): RedisCommands<String, String> {
    val host = System.getenv("REDIS_HOST")
    val port = System.getenv("REDIS_PORT")?.toIntOrNull() ?: 6379
    val ssl = System.getenv("REDIS_SSL")?.toBoolean() ?: false
    println("cache-proxy init: Redis host=$host port=$port ssl=$ssl")
    val uri = RedisURI.builder()
        .withHost(host)
        .withPort(port)
        .withSsl(ssl)
        .withVerifyPeer(SslVerifyMode.NONE)
        .withTimeout(Duration.ofSeconds(2))
        .build()
    return RedisClient.create(uri).connect().sync()
}
