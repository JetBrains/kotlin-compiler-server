package com.compiler.server.configuration

import com.compiler.server.model.VersionInfo
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.core.io.ResourceLoader
import org.springframework.format.FormatterRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.io.File

@Configuration
class ApplicationConfiguration(
  @Value("\${policy.file}") private val policyFileName: String,
  private val resourceLoader: ResourceLoader
) : WebMvcConfigurer {
  override fun addFormatters(registry: FormatterRegistry) {
    registry.addConverter(ProjectConverter())
  }

  @Bean
  fun policyFile(): File = resourceLoader.getResource(ResourceLoader.CLASSPATH_URL_PREFIX + policyFileName).file
}

@Configuration
@PropertySource(value = ["libraries.properties"], ignoreResourceNotFound = true)
@EnableConfigurationProperties(value = [LibrariesFolderProperties::class])
class KotlinLibrariesConfiguration(@Value("\${kotlin.version}") private val version: String) {
  @Bean
  fun versionInfo() = VersionInfo(version = version, stdlibVersion = version)
}

@ConfigurationProperties(prefix = "libraries.folder")
class LibrariesFolderProperties {
  lateinit var jvm: String
  lateinit var js: String
}