package com.compiler.server.validation

import com.compiler.server.model.ProjectType
import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

/**
 * Annotation representing a validation constraint for a project run request.
 *
 * This constraint validates the compiler arguments associated with a project
 * to ensure they meet specific validity requirements based on the project's configuration.
 * The validation is performed using a custom validator, `ProjectRunRequestValidator`.
 *
 * NOTE: this annotation lives in the root module rather than `:common` because the
 * referenced validator depends on Spring-managed beans that are not available in
 * `:common`. Request DTOs that live in `:common` (e.g. `TranslateComposeWasmRequest`)
 * therefore cannot use this annotation; their endpoints validate imperatively via
 * `CompilerArgumentsValidator` in the controller.
 */
@Constraint(validatedBy = [ProjectRunRequestValidator::class])
@Target(AnnotationTarget.FIELD)
annotation class CompilerArgumentsConstraint(
    val projectType: ProjectType,
    val message: String = "Invalid compiler arguments passed.",
    val groups: Array<KClass<*>> = [],
    vararg val payload: KClass<out Payload> = []
)
