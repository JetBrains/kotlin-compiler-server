package com.compiler.server.validation

import jakarta.validation.Configuration as ValidationConfig
import org.hibernate.validator.HibernateValidatorConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean

@Configuration
class ValidatorConfiguration {

    /**
     * [CompilerArgumentsConstraint] lives in `:common` and declares
     * `validatedBy = []` because the validator implementation depends on
     * Spring-managed beans that are not available in `:common`. This
     * registers the mapping programmatically at the validator-factory level,
     * replacing Spring Boot's default [LocalValidatorFactoryBean].
     */
    @Bean
    fun validator(): LocalValidatorFactoryBean = object : LocalValidatorFactoryBean() {
        override fun postProcessConfiguration(configuration: ValidationConfig<*>) {
            super.postProcessConfiguration(configuration)
            if (configuration is HibernateValidatorConfiguration) {
                val mapping = configuration.createConstraintMapping()
                mapping.constraintDefinition(CompilerArgumentsConstraint::class.java)
                    .validatedBy(ProjectRunRequestValidator::class.java)
                configuration.addMapping(mapping)
            }
        }
    }
}
