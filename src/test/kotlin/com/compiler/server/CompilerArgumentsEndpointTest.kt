package com.compiler.server

import com.compiler.server.common.components.KotlinEnvironment
import com.compiler.server.model.ProjectType
import com.compiler.server.model.bean.VersionInfo
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpMethod
import org.springframework.http.RequestEntity
import org.springframework.web.client.RestTemplate
import java.io.File
import java.net.InetAddress
import java.net.URI
import java.nio.file.Paths
import kotlin.io.path.readText
import kotlin.test.assertEquals

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CompilerArgumentsEndpointTest {

    @Autowired
    private lateinit var versionInfo: VersionInfo

    @Autowired
    private lateinit var kotlinEnvironment: KotlinEnvironment

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Value("\${local.server.port}")
    private var port = 0

    private val host: String = InetAddress.getLocalHost().hostAddress

    private fun baseUrl(): String = "http://$host:$port"

    @ParameterizedTest
    @EnumSource(ProjectType::class, mode = EnumSource.Mode.EXCLUDE, names = ["JS", "CANVAS"])
    fun `compiler arguments endpoint returns flattened data without nested argument`(projectType: ProjectType) {
        val version = versionInfo.version
        val urls = listOf(
            "/api/compiler/compiler-arguments?projectType=${projectType.id}",
            "/api/$version/compiler/compiler-arguments?projectType=${projectType.id}"
        )

        val client = RestTemplate()

        val projectTypeId = when (projectType) {
            ProjectType.JAVA, ProjectType.JUNIT -> "jvm"
            ProjectType.JS_IR, ProjectType.JS, ProjectType.CANVAS ->  "js"
            ProjectType.WASM -> "wasm"
            ProjectType.COMPOSE_WASM -> "compose-wasm"
        }

        val pluginPlaceholder = when (projectType) {
            ProjectType.JAVA, ProjectType.JUNIT -> kotlinEnvironment.compilerPlugins
            ProjectType.COMPOSE_WASM -> kotlinEnvironment.COMPOSE_WASM_COMPILER_PLUGINS.map { File(it) }
            else -> emptyList()
        }.joinToString(",") { it.name }

        val classpathPlaceholder = when (projectType) {
            ProjectType.JAVA, ProjectType.JUNIT -> kotlinEnvironment.classpath
            ProjectType.COMPOSE_WASM -> kotlinEnvironment.COMPOSE_WASM_LIBRARIES.map { File(it) }.sorted()
            else -> emptyList()
        }.joinToString(":") { it.name }

        val expectedResponseBody = Paths.get("src/test/resources/compiler-arguments/$projectTypeId-expected-compiler-args.json").readText()
            .replace("{{PLUGIN_PLACEHOLDER}}", pluginPlaceholder)
            .replace("{{CLASSPATH_PLACEHOLDER}}", classpathPlaceholder)
            .replace("{{KOTLIN_VERSION_PLACEHOLDER}}", version)

        urls.forEach { path ->
            val response = client.exchange(
                RequestEntity<Any>(HttpMethod.GET, URI.create(baseUrl() + path)),
                String::class.java
            )

            val body = response.body

            assertEquals(objectMapper.readTree(expectedResponseBody), objectMapper.readTree(body))
        }
    }
}
