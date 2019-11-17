package com.compiler.server.configuration

import com.compiler.server.model.Project
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.core.convert.converter.Converter

class ProjectConverter : Converter<String, Project> {
  override fun convert(source: String): Project = jacksonObjectMapper().readValue(source, Project::class.java)
}