package com.compiler.server.validation

import com.compiler.server.model.ProjectType
import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

/**
 * Annotation representing a validation constraint for a project run request.
 *
 * The concrete [jakarta.validation.ConstraintValidator] implementation is registered
 * programmatically in the main module (see `ValidatorConfiguration`), since the
 * validator depends on Spring-managed beans that are not available in `:common`.
 */
@Constraint(validatedBy = [])
@Target(AnnotationTarget.FIELD)
annotation class CompilerArgumentsConstraint(
    val projectType: ProjectType,
    val message: String = "Invalid compiler arguments passed.",
    val groups: Array<KClass<*>> = [],
    vararg val payload: KClass<out Payload> = []
)
