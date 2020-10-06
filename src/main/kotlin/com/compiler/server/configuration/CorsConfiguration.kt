package com.compiler.server.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter

val ACCESS_CONTROL_ALLOW_ORIGIN_VALUE: String = System.getenv("ACCESS_CONTROL_ALLOW_ORIGIN_VALUE") ?: "*"
val ACCESS_CONTROL_ALLOW_HEADER_VALUE: String = System.getenv("ACCESS_CONTROL_ALLOW_HEADER_VALUE") ?: "*"

@Configuration
class CorsConfiguration {
    @Bean
    fun corsFilter(): CorsFilter {
        val source = UrlBasedCorsConfigurationSource()
        val config = CorsConfiguration().apply {
            addAllowedHeader(ACCESS_CONTROL_ALLOW_HEADER_VALUE)
            addAllowedOrigin(ACCESS_CONTROL_ALLOW_ORIGIN_VALUE)
            addAllowedMethod("GET")
            addAllowedMethod("POST")
        }
        source.registerCorsConfiguration("/**", config)
        return CorsFilter(source)
    }
}
