package com.compiler.server.controllers

import com.compiler.server.compiler.components.KotlinCompiler
import com.compiler.server.compiler.model.JavaExecutionResult
import com.compiler.server.compiler.model.Project
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class RunRestController(
        private val kotlinCompiler: KotlinCompiler
) {
    @PostMapping("/api/compiler/run")
    fun executeKotlinProjectEndpoint(@RequestBody project: Project): JavaExecutionResult = kotlinCompiler.run(project)
}