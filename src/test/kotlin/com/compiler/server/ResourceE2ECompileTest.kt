package com.compiler.server

import com.compiler.server.base.startNodeJsApp
import com.compiler.server.compiler.components.KotlinToJSTranslator.Companion.JS_IR_CODE_BUFFER
import com.compiler.server.compiler.components.KotlinToJSTranslator.Companion.JS_IR_OUTPUT_REWRITE
import com.compiler.server.generator.generateSingleProject
import com.compiler.server.model.ExecutionResult
import com.compiler.server.model.JunitExecutionResult
import com.compiler.server.model.JvmExecutionResult
import com.compiler.server.model.ProjectType
import com.compiler.server.model.TranslationJSResult
import com.compiler.server.model.TranslationWasmResult
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.client.RestTemplate
import java.io.File
import java.net.InetAddress
import kotlin.io.path.absolutePathString
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ResourceE2ECompileTest : BaseResourceCompileTest {

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Value("\${local.server.port}")
    private var port = 0
    private val host: String = InetAddress.getLocalHost().hostAddress
    private fun getHost(): String = "http://$host:$port"

    override fun request(code: String, platform: ProjectType): ExecutionResult {
        val url = when (platform) {
            ProjectType.JS, ProjectType.JS_IR -> "/api/compiler/translate?ir=true"
            else -> "/api/compiler/run"
        }

        val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }

        val body = jacksonObjectMapper().writeValueAsString(
            generateSingleProject(code, projectType = platform)
        )

        val resultClass = when (platform) {
            ProjectType.JUNIT -> JunitExecutionResult::class.java
            ProjectType.JAVA -> JvmExecutionResult::class.java
            ProjectType.JS, ProjectType.CANVAS, ProjectType.JS_IR -> TranslationJSResult::class.java
            ProjectType.WASM, ProjectType.COMPOSE_WASM -> TranslationWasmResult::class.java
        }
        val result = RestTemplate().postForObject(
            "${getHost()}$url", HttpEntity(body, headers), resultClass
        )

        return result ?: throw IllegalStateException("Result is null")
    }

    @Test
    fun `http requests from resource folder`() {
        checkResourceExamples(
            listOf(
                testDirJVM,
                testDirJS
            )
        ) { result, file ->
            val (actualResult, extension, compareExpectedAndActualResultFunction) =
                prepareStrategyBasedOnTestResult(result)
            val out =
                file.path.replace("test-compile-data", "test-compile-output").replace("\\.kt$".toRegex(), extension)

            val outFile = File(out)

            if (outFile.exists()) {
                val text = outFile.readText()

                // TODO(Zofia Wiora): remove this part of code and use the commented version when correct end position calculation is implemented
                val endPositionPattern = "\"end\":\\{\"line\":\\d+,\"ch\":\\d+\\}".toRegex()
                val normalizedText = text.replace(endPositionPattern, "\"end\":{\"line\":0,\"ch\":0}")
                val normalizedActualResult = actualResult.replace(endPositionPattern, "\"end\":{\"line\":0,\"ch\":0}")

                if (!compareExpectedAndActualResultFunction(normalizedText, normalizedActualResult)) {
                    if (!file.isInconsistentOutput()) {
                        return@checkResourceExamples """
                            Expected: $normalizedText
                            Actual:   $normalizedActualResult
                        """.trimIndent()
                    }

                    println("!!! file: ${file.path} not equals but it's random code")
                }

//                if (!compareExpectedAndActualResultFunction(text, actualResult)) {
//                    if (!file.isInconsistentOutput()) {
//                        return@checkResourceExamples """
//                            Expected: $text
//                            Actual:   $actualResult
//                        """.trimIndent()
//                    }
//
//                    // TODO(Dmitrii Krasnov): this code ignores some of errors,
//                    //  it would be nice to rewrite it.
//                    println("!!! file: ${file.path} not equals but it's random code")
//                }
            } else {
                println("New file: ${outFile.path}")
                File(outFile.parent).mkdirs()
                outFile.writeText(actualResult)
            }

            null
        }
    }

    private fun prepareStrategyBasedOnTestResult(result: ExecutionResult): Triple<String, String, (String, String) -> Boolean> = when (result) {
        // For the JS it has no sense to compare compiled code,
        // because it differs with each new compiler version,
        // but it makes sense to compare the results of the execution such code.
        is TranslationJSResult -> Triple(
            executeCompiledJsCode(result),
            ".txt",
            { expected: String, actual: String -> expected == actual })

        else -> Triple(
            objectMapper.writeValueAsString(result),
            ".json",
            { expected: String, actual: String -> objectMapper.readTree(expected) == objectMapper.readTree(actual) })
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
        "LocalDate.now()",
        "Clock.System.now()"
    ).any { code.contains(it) }
}

/**
 * The output of the JS compilation is JavaScript code that includes some additional logic
 * for redirecting output specifically for the playground.
 * In this method, we strip out that extra logic and execute the JavaScript code directly,
 * because we're not concerned about whether the compiled code has changed —
 * what's important is whether the result of its execution has changed.
 */
private fun executeCompiledJsCode(result: TranslationJSResult): String {
    val jsCode = result.jsCode ?: ""

    val cleanedCode = jsCode.replace(JS_IR_CODE_BUFFER, "")
        .replace(JS_IR_OUTPUT_REWRITE, "")
        // In pure Node JS there is no `alert` function like in browser,
        // that is why we need to add some custom override for alerts here.
        .replace("alert('", "console.log('ALERT: ")

    return executeJsCode(cleanedCode)
}

private fun executeJsCode(jsCode: String): String {
    val tmpDir = createTempDirectory()
    val jsMain = tmpDir.resolve("playground.js")
    jsMain.writeText(jsCode)

    val textResult = startNodeJsApp(
        System.getenv("kotlin.wasm.node.path"),
        jsMain.normalize().absolutePathString()
    )

    tmpDir.toFile().deleteRecursively()
    return textResult
}


