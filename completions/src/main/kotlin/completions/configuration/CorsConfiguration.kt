package completions.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource
import org.springframework.web.cors.reactive.CorsWebFilter

@Configuration
class CorsConfiguration {
    @Bean
    fun corsFilter(): CorsWebFilter {
        val source = UrlBasedCorsConfigurationSource()
        val config = CorsConfiguration().applyPermitDefaultValues()
        source.registerCorsConfiguration("/**", config)
        return CorsWebFilter(source)
    }
}
