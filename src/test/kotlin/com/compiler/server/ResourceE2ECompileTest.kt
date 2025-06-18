package com.compiler.server

import com.compiler.server.compiler.components.KotlinToJSTranslator.Companion.JS_IR_CODE_BUFFER
import com.compiler.server.compiler.components.KotlinToJSTranslator.Companion.JS_IR_OUTPUT_REWRITE
import com.compiler.server.generator.generateSingleProject
import com.compiler.server.model.*
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.HostAccess
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.client.RestTemplate
import java.io.File
import java.io.OutputStream
import java.net.InetAddress

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ResourceE2ECompileTest : BaseResourceCompileTest {
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
            val (actualResult, extension) = when (result) {
                // For the JS it has no sense to compare compiled code,
                // because it differs with each new compiler version,
                // but it makes sense to compare the results of the execution such code.
                is TranslationJSResult -> Pair(executeCompiledJsCode(result), ".txt")
                else -> Pair(jacksonObjectMapper().writeValueAsString(result), ".json")
            }
            val out =
                file.path.replace("test-compile-data", "test-compile-output").replace("\\.kt$".toRegex(), extension)

            val outFile = File(out)

            if (outFile.exists()) {
                val text = outFile.readText()
                if (text != actualResult) {
                    if (!file.isInconsistentOutput()) {
                        return@checkResourceExamples """
                            Expected: $text
                            Actual:   $actualResult
                        """.trimIndent()
                    }

                    // TODO(Dmitrii Krasnov): this code ignores some of errors,
                    //  it would be nice to rewrite it.
                    println("!!! file: ${file.path} not equals but it's random code")
                }
            } else {
                println("New file: ${outFile.path}")
                File(outFile.parent).mkdirs()
                outFile.writeText(actualResult)
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
        "LocalDate.now()",
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

    val output = StringBuilder()
    val context = Context.newBuilder("js")
        .option("js.console", "true")
        .allowHostAccess(HostAccess.ALL)
        .allowAllAccess(true)
        .out(object : OutputStream() {
            override fun write(b: Int) {
                output.append(b.toChar())
            }
        })
        .build()

    // In pure GraalVM JS there is no `alert` function like in browser,
    // that is why we need to add some interception logic for it.
    context.getBindings("js").putMember("alert", ProxyExecutable { args ->
        output.append("ALERT: ").append(args.joinToString()).append("\n")
        null
    })

    context.eval("js", cleanedCode)
    return output.toString()
}
