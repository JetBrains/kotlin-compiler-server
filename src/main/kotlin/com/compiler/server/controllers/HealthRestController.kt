package com.compiler.server.controllers

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class HealthRestController {
  @GetMapping("/health")
  fun healthEndpoint() = "OK"
}