package com.compiler.server.compiler.components

import com.compiler.server.model.bean.LibrariesFile
import component.KotlinEnvironment
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class KotlinEnvironmentConfiguration(val librariesFile: LibrariesFile) {
  @Bean
  fun kotlinEnvironment(): KotlinEnvironment {
    val classPath =
      listOfNotNull(librariesFile.jvm)
        .flatMap {
          it.listFiles()?.toList()
            ?: error("No kotlin libraries found in: ${librariesFile.jvm.absolutePath}")
        }

    val additionalJsClasspath = listOfNotNull(librariesFile.js)
    return KotlinEnvironment(classPath, additionalJsClasspath, listOfNotNull(librariesFile.wasm))
  }
}
