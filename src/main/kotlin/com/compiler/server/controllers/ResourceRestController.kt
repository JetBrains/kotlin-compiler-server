package com.compiler.server.controllers

import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping(value = ["/api/resource", "/api/**/resource"])
class ResourceRestController(
    @Value("\${skiko.version}") private val skikoVersion: String,
    @Value("\${dependencies.compose.wasm}") private val dependenciesComposeWasm: String,
) {
    @Suppress("unused")
    @GetMapping("/compose-wasm-versions")
    fun getVersions(): Map<String, String> {
        return mapOf(
            "skiko" to skikoVersion,
            "stdlib" to dependenciesComposeWasm
        )
    }
}
