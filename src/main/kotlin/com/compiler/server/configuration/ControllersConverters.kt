package com.compiler.server.configuration

import com.compiler.server.model.Project
import com.compiler.server.model.ProjectType
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.core.convert.converter.Converter
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

class ProjectConverter : Converter<String, Project> {
  override fun convert(source: String): Project = jacksonObjectMapper().readValue(source, Project::class.java)
}

class ProjectTypeConverter : Converter<String, ProjectType> {
    override fun convert(source: String): ProjectType =
        ProjectType.entries.firstOrNull { it.id == source }
            ?: throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Unknown projectType '$source'"
            )
}