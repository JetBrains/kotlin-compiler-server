package com.compiler.server.validation

import com.compiler.server.model.ProjectType
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import org.springframework.beans.factory.annotation.Autowired

class ProjectRunRequestValidator : ConstraintValidator<CompilerArgumentsConstraint?, Map<String, Any>> {
    @Autowired
    private lateinit var compilerArgumentsValidators: Map<ProjectType, AbstractCompilerArgumentsValidator>

    private var projectType: ProjectType = ProjectType.JAVA

    override fun initialize(constraintAnnotation: CompilerArgumentsConstraint?) {
        projectType = constraintAnnotation?.projectType
            ?: throw IllegalArgumentException("Project type must be provided for compiler args validation.")
    }

    override fun isValid(
        compilerArguments: Map<String, Any>,
        cxt: ConstraintValidatorContext
    ): Boolean {
        return compilerArgumentsValidators[projectType]
            ?.validateCompilerArguments(compilerArguments)
            ?: false
    }
}
