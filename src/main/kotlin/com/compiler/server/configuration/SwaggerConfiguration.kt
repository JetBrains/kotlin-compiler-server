package com.compiler.server.configuration

import com.compiler.server.controllers.CompilerRestController
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import springfox.documentation.builders.PathSelectors
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket

@Configuration
class SwaggerConfiguration {
    @Bean
    fun apiDocket(): Docket {
        return Docket(DocumentationType.OAS_30)
            .select()
            // If controllers are in different packages we should add selectors for these packages as well
            .apis(RequestHandlerSelectors.basePackage(CompilerRestController::class.java.packageName))
            .paths(PathSelectors.any())
            .build()
    }
}