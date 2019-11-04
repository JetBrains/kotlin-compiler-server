package com.compiler.server.compiler.components

import com.compiler.server.compiler.KotlinFile
import com.compiler.server.compiler.model.Completion
import com.compiler.server.compiler.model.JavaExecutionResult
import com.compiler.server.compiler.model.Project
import org.apache.commons.logging.LogFactory
import org.springframework.stereotype.Component

@Component
class KotlinProjectExecutor(
        private val kotlinCompiler: KotlinCompiler,
        private val kotlinEnvironment: KotlinEnvironment
) {

    private val log = LogFactory.getLog(KotlinProjectExecutor::class.java)

    fun run(project: Project): JavaExecutionResult {
        val files = getFilesFrom(project)
        return kotlinCompiler.run(files)
    }

    fun complete(project: Project, line: Int, character: Int): List<Completion> {
        val file = getFilesFrom(project).first()
        return try {
            kotlinEnvironment.complete(file, line, character)
        } catch (e: Exception) {
            log.warn("Exception in getting completions", e)
            emptyList()
        }
    }

    private fun getFilesFrom(project: Project) = project.files.map {
        KotlinFile.from(kotlinEnvironment.kotlinEnvironment.project, it.name, it.text)
    }
}