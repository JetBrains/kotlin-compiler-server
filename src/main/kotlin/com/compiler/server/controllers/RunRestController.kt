package com.compiler.server.controllers

import com.compiler.server.compiler.KotlinCompiler
import com.compiler.server.compiler.KotlinEnvironment
import com.compiler.server.compiler.KotlinFile
import com.compiler.server.compiler.model.JavaExecutionResult
import com.compiler.server.compiler.model.Project
import com.compiler.server.compiler.model.Severity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class RunRestController(
        private val environment: KotlinEnvironment,
        private val kotlinCompiler: KotlinCompiler
) {

    @PostMapping("/foo")
    fun foo(@RequestBody project: Project): JavaExecutionResult {
        val files = project.files.map {
            KotlinFile.from(environment.kotlinEnvironment.project, it.name, it.text)
        }
        val errors = environment.errorsFrom(files.map { it.kotlinFile })
        return if (errors.any { it.value.any { error -> error.severity == Severity.ERROR } })
            JavaExecutionResult("", errors = errors)
        else {
            val compilation = kotlinCompiler
                    .compile(files.map { it.kotlinFile })
            return kotlinCompiler.execute(compilation)
        }
    }
}