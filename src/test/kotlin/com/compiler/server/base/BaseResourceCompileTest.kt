package com.compiler.server

import com.compiler.server.model.ExecutionResult
import com.compiler.server.model.ProjectType
import java.io.File
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

interface BaseResourceCompileTest {
    val testDirJVM: String
        get() = "src/test/resources/test-compile-data/jvm"

    val testDirJS: String
        get() = "src/test/resources/test-compile-data/js"

    fun request(code: String, platform: ProjectType): ExecutionResult

    fun checkResourceExamples(folders: List<String>, verifyResult: (ExecutionResult, File) -> String?) {
        val badMap = mutableMapOf<String, String>()

        folders.forEach { folder ->
            val testFiles = File(folder).walk().toList().filter { it.isFile && it.extension == "kt" }

            assertNotNull(folder) { "Can not init test directory" }
            assertTrue(testFiles.isNotEmpty(), "No files in test directory")

            testFiles.forEach { file ->
                val code = file.readText()

                val platform: ProjectType = when (folder) {
                    testDirJVM -> ProjectType.JAVA
                    testDirJS -> ProjectType.JS_IR
                    else -> throw IllegalArgumentException("Unknown type $folder")
                }

                val result = request(code, platform)

                verifyResult(result, file)?.run {
                    val key = "${platform.name}:${file.path.substring(folder.length + 1)}"
                    badMap[key] = badMap[key].orEmpty() + this
                }
            }
        }

        if (badMap.isNotEmpty()) {
            val message = badMap.entries
                .sortedBy { it.key }
                .joinToString("\n") {
                    "File: ${it.key}. Error:\n${it.value}"
                }

            error("Compile tests failed.\nResults:\n$message")
        }
    }
}
