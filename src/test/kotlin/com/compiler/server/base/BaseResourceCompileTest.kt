package com.compiler.server

import com.compiler.server.model.ExecutionResult
import com.compiler.server.model.ProjectType
import java.io.File
import java.security.MessageDigest
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private val FILES_TO_USE_WORKAROUND = mapOf(
    "5ec261721162729866abc1e8f7293ef4.31" to "a2c8dd6ff3a6074c01c7bb813cc2cbfb",
    "5ec261721162729866abc1e8f7293ef4.32" to "69496b10b87ba1491dff96f2396c5b40",
    "8a72411880f54ee0a305b57ed6c6901d.2" to "82e5ecc6747c91c85b697bd43cf93c93",
    "4c8b9a374d2df52b3a804a9b8e54ee2b.20" to "e58b9c7e6996fcfd2a53da5373c683ba",
)

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

                val platform: ProjectType = when (folder) {
                    testDirJVM -> ProjectType.JAVA
                    testDirJS -> ProjectType.JS_IR
                    else -> throw IllegalArgumentException("Unknown type $folder")
                }

                // Decide whether to use workaround expected output based on hash table
                val expectedResultFile = getExpectedResultFile(platform, file, folder, badMap) ?: return@forEach
                val code = expectedResultFile.readText()

                val result = request(code, platform)

                verifyResult(result, expectedResultFile)?.run {
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

    fun getExpectedResultFile(
        platform: ProjectType,
        file: File,
        folder: String,
        badMap: MutableMap<String, String>,
    ): File? {
        val fileName = file.nameWithoutExtension
        val expectedHash = FILES_TO_USE_WORKAROUND[fileName]
        return if (expectedHash != null) {
            val actualHash = md5Hex(file.readText())
            if (!actualHash.equals(expectedHash, ignoreCase = true)) {
                val key = "${platform.name}:${file.path.substring(folder.length + 1)}"
                val msg =
                    "Workaround hash mismatch. Actual MD5: $actualHash, expected: $expectedHash. Please review the source and update FILES_TO_USE_WORKAROUND entry and/or the *-workaround.json accordingly.\n"
                badMap[key] = badMap[key].orEmpty() + msg
                null
            } else {
                File(file.parent.replace("kotlin-web-site", "workaround-examples"), "${fileName}-workaround.kt")
            }
        } else {
            file
        }
    }

    private fun md5Hex(text: String): String {
        return MessageDigest.getInstance("MD5")
            .digest(text.toByteArray())
            .toHexString(HexFormat.Default)
    }
}
