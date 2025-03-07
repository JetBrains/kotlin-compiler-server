package com.compiler.server.controllers

import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.*
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

    @Suppress("unused")
    @GetMapping("/skiko")
    fun getVersionedSkikoMjs(): String {
        return skikoVersion
    }

  @GetMapping("/skiko.mjs")
  fun getSkikoMjs(): ResponseEntity<Resource> {
    return nonCacheableResource("/com/compiler/server/skiko.mjs", MediaType("text", "javascript"))
  }

  @GetMapping("/skiko.wasm")
  fun getSkikoWasm(): ResponseEntity<Resource> {
    return nonCacheableResource("/com/compiler/server/skiko.wasm", MediaType("application", "wasm"))
  }

  private fun nonCacheableResource(path: String, mediaType: MediaType): ResponseEntity<Resource> {
    return resource(path, mediaType)
  }

  @Suppress("unused")
  @GetMapping("/skiko-{version}.mjs")
  fun getVersionedSkikoMjs(@Value("\${kotlin.version}") version: String): ResponseEntity<Resource> {
    return cacheableResource("/com/compiler/server/skiko.mjs", MediaType("text", "javascript"))
  }

  @Suppress("unused")
  @GetMapping("/skiko-{version}.wasm")
  fun getVersionedSkikoWasm(@Value("\${kotlin.version}") version: String): ResponseEntity<Resource> {
    return cacheableResource("/com/compiler/server/skiko.wasm", MediaType("application", "wasm"))
  }

  @GetMapping("/stdlib.mjs")
  fun getStdlibMjs(): ResponseEntity<Resource> {
    val resource = FileSystemResource("/Users/ilya.goncharov/repos/kotlin-compiler-server/COMPOSE/stdlib.uninstantiated.mjs")
    val headers = HttpHeaders().apply {
      contentType = MediaType("text", "javascript")
    }

    return ResponseEntity(resource, headers, HttpStatus.OK)
  }

    @GetMapping("/stdlib")
    fun getStdlib(): String {
        return dependenciesComposeWasm
    }

  @GetMapping("/stdlib.wasm")
  fun getStdlibWasm(): ResponseEntity<Resource> {
    val resource = FileSystemResource("/Users/ilya.goncharov/repos/kotlin-compiler-server/COMPOSE/stdlib.wasm")
    val headers = HttpHeaders().apply {
      contentType = MediaType("application", "wasm")
    }

    return ResponseEntity(resource, headers, HttpStatus.OK)
  }

  private fun cacheableResource(path: String, mediaType: MediaType): ResponseEntity<Resource> {
    return resource(path, mediaType) {
      cacheControl = CacheControl.maxAge(365, TimeUnit.DAYS).headerValue
    }
  }

  private fun resource(
    path: String,
    mediaType: MediaType,
    headers: HttpHeaders.() -> Unit = {},
  ): ResponseEntity<Resource> {
    val resourcePath = javaClass.getResource(path)?.path
      ?: return ResponseEntity.internalServerError().build()

    val resource = FileSystemResource(resourcePath)
    val headers = HttpHeaders().apply {
      contentType = mediaType
      headers()
    }

    return ResponseEntity(resource, headers, HttpStatus.OK)
  }
}
