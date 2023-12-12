package com.compiler.server

import com.compiler.server.generator.generateSingleProject
import com.compiler.server.model.ExecutionResult
import com.compiler.server.model.ProjectType
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.client.RestTemplate
import java.io.File
import java.net.InetAddress

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ResourceE2ECompileTest : BaseResourceCompileTest {
    @Value("\${local.server.port}")
    private var port = 0
    private val host: String = InetAddress.getLocalHost().hostAddress
    private fun getHost(): String = "http://$host:$port"

    override fun request(code: String, platform: ProjectType): ExecutionResult {
        val url = when (platform) {
            ProjectType.JS, ProjectType.JS_IR ->  "/api/compiler/translate?ir=true"
            else -> "/api/compiler/run"
        }

        val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }

        val body = jacksonObjectMapper().writeValueAsString(
            generateSingleProject(code, projectType = platform)
        )

        val result = RestTemplate().postForObject(
            "${getHost()}$url", HttpEntity(body, headers), ExecutionResult::class.java
        )

        return result ?:
            throw IllegalStateException("Result is null")
    }

    @Test
    fun `http requests from resource folder`() {
        checkResourceExamples(listOf(testDirJVM, testDirJS)) { result, file ->
            val json = jacksonObjectMapper().writeValueAsString(result)
            val out = file.path.replace("test-compile-data", "test-compile-output").replace("\\.kt$".toRegex(), ".json")

            val outFile = File(out)

            if (outFile.exists()) {
                val text = outFile.readText()
                if (text != json) {
                    if (!file.isInconsistentOutput()) {
                        return@checkResourceExamples """
                            Expected: $text
                            Actual:   $json
                        """.trimIndent()
                    }

                    println("!!! file: ${file.path} not equals but it's random code")
                }
            } else {
                println("New file: ${outFile.path}")
                File(outFile.parent).mkdirs()
                outFile.writeText(json)
            }

            null
        }
    }
}

private fun File.isInconsistentOutput(): Boolean {
    val code = this.readText()

    return listOf(
        "import java.util.Random",
        "import kotlin.random.Random",
        ".shuffle()",
        ".shuffled()",
        ".random()",
        ".nextInt(",
        ".markNow()",
        ".elapsedNow()",
        ".hasPassedNow()",
        ".inWholeNanoseconds",
        " measureTime {",
        " measureTimedValue {",
    ).any { code.contains(it) }
}
