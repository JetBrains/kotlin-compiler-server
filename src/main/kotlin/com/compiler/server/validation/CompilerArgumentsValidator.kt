package com.compiler.server.validation

import com.compiler.server.model.ProjectType
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException

@Component
class CompilerArgumentsValidator(
    private val perProjectType: Map<ProjectType, AbstractCompilerArgumentsValidator>,
) {
    fun ensureValid(projectType: ProjectType, vararg argMaps: Map<String, Any>) {
        val validator = perProjectType[projectType]
            ?: error("No compiler-arguments validator registered for $projectType")
        for (args in argMaps) {
            if (!validator.validateCompilerArguments(args)) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid compiler arguments passed.")
            }
        }
    }
}
