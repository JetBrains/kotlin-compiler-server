package com.compiler.server

import com.compiler.server.generator.generateSingleProject
import com.compiler.server.model.ExecutionResult
import com.compiler.server.model.bean.VersionInfo
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.client.RestTemplate
import java.net.InetAddress
import kotlin.test.assertContains
import kotlin.test.assertNotNull

@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class CompilerAPITest {

  @Value("\${local.server.port}")
  private var port = 0

  private val host: String = InetAddress.getLocalHost().hostAddress

  @Autowired
  private lateinit var versionInfo: VersionInfo

  companion object {
    private const val PROGRAM_RUN = "fun main() {\n println(\"Hello, world!!!\")\n}"
  }

  @Test
  fun `run api kotlin snippet`() {
    val version = versionInfo.version
    listOf(
      "/api/compiler/run",
      "/api/$version/compiler/run"
    ).forEach { url ->

      val headers = HttpHeaders()
      headers.contentType = MediaType.APPLICATION_JSON
      val response = RestTemplate().postForObject(
        getHost() + url,
        HttpEntity(
          jacksonObjectMapper().writeValueAsString(
            generateSingleProject(PROGRAM_RUN)
          ),
          headers
        ),
        ExecutionResult::class.java
      )
      assertNotNull(response, "Empty response!")
      assertContains(
        response.text,
        "Hello, world!!!",
      )
    }
  }


  private fun getHost(): String = "http://$host:$port"

}