package com.compiler.server.controllers

import com.compiler.server.compiler.JavaExecutor
import com.compiler.server.compiler.KotlinCompiler
import com.compiler.server.compiler.KotlinEnvironment
import com.compiler.server.compiler.KotlinFile
import com.compiler.server.compiler.model.ExceptionDescriptor
import com.compiler.server.compiler.model.JavaExecutionResult
import com.compiler.server.compiler.model.Project
import com.compiler.server.compiler.model.Severity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

@RestController
class RunRestController {

    data class OutputDir(val path: Path, val subPaths: List<Path>)

    @PostMapping("/foo")
    fun foo(@RequestBody project: Project): JavaExecutionResult {
        val environment = KotlinEnvironment
                .with(classpath = listOfNotNull(File("lib"))
                        .flatMap { it.listFiles().toList() })
        val files = project.files.map {
            KotlinFile.from(environment.kotlinEnvironment.project, it.name, it.text)
        }
        val errors = environment.errorsFrom(files.map { it.kotlinFile })
        return if (errors.any { it.value.any { error -> error.severity == Severity.ERROR } })
            JavaExecutionResult("", errors = errors)
        else {
            val compilation = KotlinCompiler(environment)
                    .compile(files.map { it.kotlinFile })
            if (compilation.files.isNotEmpty()) {
                val output = write(compilation)
                JavaExecutor.execute(argsFrom(compilation.mainClass!!, output, environment))
                        .also { output.path.toAbsolutePath().toFile().deleteRecursively() }
            } else JavaExecutionResult("", ExceptionDescriptor("Something went wrong", "Exception"))
        }
    }

    private fun argsFrom(mainClass: String, outputDirectory: OutputDir, environment: KotlinEnvironment) = listOfNotNull(
            "java",
            "-Djava.security.manager",
            "-Djava.security.policy=${outputDirectory.path.resolve("executor.policy").toAbsolutePath()}",
            "-classpath"
    ) + (environment.classpath.map { it.absolutePath } + outputDirectory.path.toAbsolutePath().toString()).joinToString(":") +
            mainClass


    private fun write(compiled: KotlinCompiler.Compiled): OutputDir {
        val dir = System.getProperty("user.dir")
        val sessionId = UUID.randomUUID().toString().replace("-", "")
        val outputDir = Paths.get(dir, "generated", sessionId)
        val policy = File("executor.policy").readText()
                .replace("%%GENERATED%%", outputDir.toString())
                .replace("%%LIB_DIR%%", dir)
        outputDir.resolve("executor.policy").apply { parent.toFile().mkdirs() }.toFile().writeText(policy)
        return OutputDir(outputDir, compiled.files.map { (name, bytes) ->
            outputDir.resolve(name).let { path ->
                path.parent.toFile().mkdirs()
                Files.write(path, bytes)
            }
        })
    }

}