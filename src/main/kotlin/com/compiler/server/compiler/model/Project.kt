package com.compiler.server.compiler.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Project(
        val id: String = "",
        val name: String = "",
        val args: String = "",
        val files: List<ProjectFile> = listOf(),
        val confType: String = "java",
        val originUrl: String? = null,
        val readOnlyFileNames: List<String> = emptyList(),
        val expectedOutput: String? = null,
        val compilerVersion: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ProjectFile(
        val text: String = "",
        val name: String = "",
        val publicId: String = "",
        val type: Type = Type.KOTLIN_FILE,
        val modifiable: Boolean = true
)

enum class Type {
    KOTLIN_FILE,
    KOTLIN_TEST_FILE,
    SOLUTION_FILE,
    JAVA_FILE
}