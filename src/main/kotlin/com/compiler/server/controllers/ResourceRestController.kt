package com.compiler.server.controllers

import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.*
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.TimeUnit


@RestController
@RequestMapping(value = ["/api/resource", "/api/**/resource"])
class ResourceRestController {
  @GetMapping("/skiko.mjs")
  fun getSkikoMjs(): ResponseEntity<Resource> {
    return cacheableResource("/com/compiler/server/skiko.mjs", MediaType("text", "javascript"))
  }

  @GetMapping("/skiko.wasm")
  fun getSkikoWasm(): ResponseEntity<Resource> {
    return cacheableResource("/com/compiler/server/skiko.wasm", MediaType("application", "wasm"))
  }

  private fun cacheableResource(path: String, mediaType: MediaType): ResponseEntity<Resource> {
    val resourcePath = javaClass.getResource(path)?.path
      ?: return ResponseEntity.internalServerError().build()

    val resource = FileSystemResource(resourcePath)
    val headers = HttpHeaders().apply {
      contentType = mediaType
      cacheControl = CacheControl.maxAge(365, TimeUnit.DAYS).headerValue
    }

    return ResponseEntity(resource, headers, HttpStatus.OK)
  }
}
