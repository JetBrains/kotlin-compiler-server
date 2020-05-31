package com.compiler.server.controllers

import org.intellij.lang.annotations.Language
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
object MainPage {
    @Language("HTML")
    @GetMapping("/")
    fun main() = """
        <!DOCTYPE html>
        <html>
          <head>
            <meta charset="UTF-8">
            <title>Kotlin Playground: Edit, Run, Share Kotlin Code Online</title>
          </head>
          <body>
          <script type="text/javascript" src="/main.js"></script></body>
        </html>
    """.trimIndent()
}