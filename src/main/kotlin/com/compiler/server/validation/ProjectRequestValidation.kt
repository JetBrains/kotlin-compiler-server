package com.compiler.server.validation

import com.compiler.server.model.ProjectType
import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import org.springframework.beans.factory.annotation.Autowired
import kotlin.reflect.KClass

/**
 * Annotation representing a validation constraint for a project run request.
 *
 * This constraint validates the compiler arguments associated with a project
 * to ensure they meet specific validity requirements based on the project's configuration.
 * The validation is performed using a custom validator, `ProjectRunRequestValidator`.
 *
 * Properties:
 * @property message The error message returned when the validation fails. Defaults to a standard message.
 * @property groups Associates this constraint with specific validation groups, if needed.
 * @property payload Provides additional metadata about the constraint for advanced use cases.
 */
@Constraint(validatedBy = [ProjectRunRequestValidator::class])
@Target(AnnotationTarget.FIELD)
annotation class CompilerArgumentsConstraint(
    val projectType: ProjectType,
    val message: String = "Invalid compiler arguments passed.",
    val groups: Array<KClass<*>> = [],
    vararg val payload: KClass<out Payload> = []
)

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