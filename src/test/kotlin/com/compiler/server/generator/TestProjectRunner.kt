package com.compiler.server.generator

import com.compiler.server.base.filterOnlyErrors
import com.compiler.server.base.hasErrors
import com.compiler.server.base.renderErrorDescriptors
import com.compiler.server.base.startNodeJsApp
import com.compiler.server.model.CompilerDiagnostics
import com.compiler.server.model.ExecutionResult
import com.compiler.server.model.JunitExecutionResult
import com.compiler.server.model.Project
import com.compiler.server.model.ProjectType
import com.compiler.server.model.TestDescription
import com.compiler.server.model.TranslationResultWithJsCode
import com.compiler.server.model.TranslationWasmResult
import com.compiler.server.service.KotlinProjectExecutor
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText


@Component
class TestProjectRunner {
    @Autowired
    private lateinit var kotlinProjectExecutor: KotlinProjectExecutor

    fun run(
        @Language("kotlin")
        code: String,
        contains: String,
        args: String = "",
        addByteCode: Boolean,
    ): ExecutionResult {
        val project = generateSingleProject(text = code, args = args)
        return runAndTest(project, contains, addByteCode)
    }

    fun multiRun(code: List<String>, contains: String, addByteCode: Boolean) {
        val project = generateMultiProject(*code.toTypedArray())
        runAndTest(project, contains, addByteCode)
    }

    fun runJs(
        @Language("kotlin")
        code: String,
        contains: String,
        args: String = "",
        convert: KotlinProjectExecutor.(Project) -> TranslationResultWithJsCode
    ) {
        val project = generateSingleProject(text = code, args = args, projectType = ProjectType.JS_IR)
        convertAndTest(project, contains, convert)
    }

    fun multiRunJs(
        code: List<String>,
        contains: String,
        convert: KotlinProjectExecutor.(Project) -> TranslationResultWithJsCode
    ) {
        val project = generateMultiProject(*code.toTypedArray(), projectType = ProjectType.JS_IR)
        convertAndTest(project, contains, convert)
    }

    fun runWasm(
        @Language("kotlin")
        code: String,
        contains: String,
    ) {
        val project = generateSingleProject(text = code, projectType = ProjectType.WASM)
        convertWasmAndTest(project, contains)
    }

    fun translateToJsIr(@Language("kotlin") code: String): TranslationResultWithJsCode {
        val project = generateSingleProject(text = code, projectType = ProjectType.JS_IR)
        return kotlinProjectExecutor.convertToJsIr(
            project,
        )
    }

    fun runWithException(
        @Language("kotlin")
        code: String,
        contains: String,
        message: String? = null,
        addByteCode: Boolean
    ): ExecutionResult {
        val project = generateSingleProject(text = code)
        val result = kotlinProjectExecutor.run(project, addByteCode)
        Assertions.assertNotNull(result.exception, "Test result should no be a null")
        Assertions.assertTrue(
            result.exception?.fullName?.contains(contains) == true,
            "Actual: ${result.exception?.message}, Expected: $contains"
        )
        if (message != null) Assertions.assertEquals(message, result.exception?.message)
        return result
    }

    fun test(@Language("kotlin") vararg test: String, addByteCode: Boolean): List<TestDescription> {
        val project = generateMultiProject(*test, projectType = ProjectType.JUNIT)
        val result = kotlinProjectExecutor.test(project, addByteCode) as? JunitExecutionResult
        Assertions.assertNotNull(result?.testResults, "Test result should no be a null")
        return result?.testResults?.values?.flatten() ?: emptyList()
    }

    fun testRaw(@Language("kotlin") vararg test: String, addByteCode: Boolean): JunitExecutionResult? =
        executeTest(*test, addByteCode = addByteCode)

    fun highlight(@Language("kotlin") code: String): CompilerDiagnostics {
        val project = generateSingleProject(text = code)
        return kotlinProjectExecutor.highlight(project)
    }

    fun highlightJS(@Language("kotlin") code: String): CompilerDiagnostics {
        val project = generateSingleProject(text = code, projectType = ProjectType.JS_IR)
        return kotlinProjectExecutor.highlight(project)
    }

    fun highlightWasm(@Language("kotlin") code: String): CompilerDiagnostics {
        val project = generateSingleProject(text = code, projectType = ProjectType.WASM)
        return kotlinProjectExecutor.highlight(project)
    }

    fun getVersion() = kotlinProjectExecutor.getVersion().version

    private fun executeTest(@Language("kotlin") vararg test: String, addByteCode: Boolean): JunitExecutionResult? {
        val project = generateMultiProject(*test, projectType = ProjectType.JUNIT)
        return kotlinProjectExecutor.test(project, addByteCode) as? JunitExecutionResult
    }

    private fun runAndTest(project: Project, contains: String, addByteCode: Boolean): ExecutionResult {
        val result = kotlinProjectExecutor.run(project, addByteCode)
        Assertions.assertNotNull(result, "Test result should no be a null")
        Assertions.assertTrue(
            result.text.contains(contains), """
      Actual: ${result.text} 
      Expected: $contains       
      Result: ${result.compilerDiagnostics}
    """.trimIndent()
        )
        return result
    }

    private fun convertAndTest(
        project: Project,
        contains: String,
        convert: KotlinProjectExecutor.(Project) -> TranslationResultWithJsCode
    ) {
        val result = kotlinProjectExecutor.convert(project)
        Assertions.assertNotNull(result, "Test result should no be a null")
        Assertions.assertFalse(result.hasErrors) {
            "Test contains errors!\n\n" + renderErrorDescriptors(result.compilerDiagnostics.filterOnlyErrors)
        }
        Assertions.assertTrue(result.jsCode!!.contains(contains), "Actual: ${result.jsCode}. \n Expected: $contains")
    }

    private fun convertWasmAndTest(
        project: Project,
        contains: String,
    ): ExecutionResult {
        val result = kotlinProjectExecutor.convertToWasm(
            project,
            debugInfo = true,
        )

        if (result !is TranslationWasmResult) {
            Assertions.assertFalse(result.hasErrors) {
                "Test contains errors!\n\n" + renderErrorDescriptors(result.compilerDiagnostics.filterOnlyErrors)
            }
        }

        result as TranslationWasmResult

        Assertions.assertNotNull(result, "Test result should no be a null")

        val tmpDir = createTempDirectory()
        val jsMain = tmpDir.resolve("playground.mjs")
        jsMain.writeText(result.jsCode)
        result.deps.forEach { (dependencyName, content) ->
            val dependency = tmpDir.resolve(dependencyName)
            dependency.writeText(content)
        }

        // this script simulates the env like in browser inside the node js
        val bootstrapBrowser = prepareBrowserSimulatorScript(tmpDir)

        val textResult = startNodeJsApp(
            System.getenv("kotlin.wasm.node.path"),
            jsMain.normalize().absolutePathString(),
            bootstrapBrowser.normalize().absolutePathString(),
        )
        tmpDir.toFile().deleteRecursively()

        Assertions.assertTrue(textResult.contains(contains), "Actual: ${textResult}. \n Expected: $contains")
        return result
    }

    private fun prepareBrowserSimulatorScript(tmpDir: Path): Path {
        val bootstrapBrowser = tmpDir.resolve("bootstrap-browser-env.mjs")
        bootstrapBrowser.writeText(
            // language=JavaScript
            """
                // bootstrap-browser-env.mjs
                import { Buffer } from 'node:buffer';
                
                // 1. Minimal browser-like globals
                
                if (typeof globalThis.window === 'undefined') {
                  globalThis.window = globalThis;
                }
                if (typeof globalThis.self === 'undefined') {
                  globalThis.self = globalThis.window;
                }
                if (typeof globalThis.document === 'undefined') {
                  globalThis.document = {};
                }
                if (typeof globalThis.navigator === 'undefined') {
                  globalThis.navigator = { userAgent: 'fake-browser-on-node' };
                }
                globalThis.process = undefined;
                
                // 2. Override atob / btoa with a tolerant implementation, required for wasm decoding
                
                globalThis.atob = (str) => {
                  const clean = String(str).replace(/[\t\n\f\r ]+/g, '');
                  return Buffer.from(clean, 'base64').toString('binary');
                };
                
                globalThis.btoa = (str) => {
                  return Buffer.from(String(str), 'binary').toString('base64');
                };
                """.trimIndent()
        )
        return bootstrapBrowser
    }
}
