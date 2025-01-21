package com.compiler.server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ResourceApplication

fun main(args: Array<String>) {
  runApplication<ResourceApplication>(*args)
}
