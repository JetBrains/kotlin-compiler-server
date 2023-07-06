package com.compiler.server.configuration

import com.compiler.server.model.bean.LibrariesFile
import com.compiler.server.model.bean.VersionInfo
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.format.FormatterRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.io.File

@Configuration
@EnableConfigurationProperties(value = [LibrariesFolderProperties::class])
class ApplicationConfiguration(
  @Value("\${kotlin.version}") private val version: String,
  private val librariesFolderProperties: LibrariesFolderProperties
) : WebMvcConfigurer {
  override fun addFormatters(registry: FormatterRegistry) {
    registry.addConverter(ProjectConverter())
  }

  @Bean
  fun versionInfo() = VersionInfo(
    version = version,
    stdlibVersion = version
  )

  @Bean
  fun librariesFiles() = LibrariesFile(
    File(librariesFolderProperties.jvm),
    File(librariesFolderProperties.js),
    File(librariesFolderProperties.wasm)
  )
}

@ConfigurationProperties(prefix = "libraries.folder")
class LibrariesFolderProperties {
  lateinit var jvm: String
  lateinit var js: String
  lateinit var wasm: String
}