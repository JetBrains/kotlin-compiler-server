package com.compiler.server.cacheproxy.service

enum class CacheEndpointType(val cacheName: String) {
    COMPOSE_WASM_V2("compose-wasm-V2"),
    COMPOSE_WASM_V1("compose-wasm-v1"),
}
