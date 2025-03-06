package com.compiler.server.controllers

import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.*
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.TimeUnit


@RestController
@RequestMapping(value = ["/api/resource", "/api/**/resource"])
class ResourceRestController(
  @Value("\${skiko.version}") private val skikoVersion: String,
  @Value("\${dependencies.compose.wasm}") private val dependenciesComposeWasm: String,
) {
  @Suppress("unused")
  @GetMapping("/skiko-{version}.mjs")
  fun getSkikoMjs(@PathVariable version: String): ResponseEntity<Resource> {
    if (version != skikoVersion) {
      throw IllegalArgumentException("Unexpected skiko version")
    }
    return cacheableResource("/com/compiler/server/skiko.mjs", MediaType("text", "javascript"))
  }

  @Suppress("unused")
  @GetMapping("/skiko-{version}.wasm")
  fun getSkikoWasm(@PathVariable version: String): ResponseEntity<Resource> {
    if (version != skikoVersion) {
      throw IllegalArgumentException("Unexpected skiko version")
    }
    return cacheableResource("/com/compiler/server/skiko.wasm", MediaType("application", "wasm"))
  }

  @GetMapping("/stdlib-{hash}.mjs")
  fun getStdlibMjs(@PathVariable hash: String): ResponseEntity<Resource> {
    if (hash != dependenciesComposeWasm) {
      throw IllegalArgumentException("Unexpected stdlib")
    }
    return cacheableResource("/com/compiler/server/stdlib_master.uninstantiated.mjs", MediaType("text", "javascript"))
  }

  @GetMapping("/stdlib-{hash}.wasm")
  fun getStdlibWasm(@PathVariable hash: String): ResponseEntity<Resource> {
    if (hash != dependenciesComposeWasm) {
      throw IllegalArgumentException("Unexpected stdlib")
    }
    return cacheableResource("/com/compiler/server/stdlib_master.wasm", MediaType("application", "wasm"))
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
