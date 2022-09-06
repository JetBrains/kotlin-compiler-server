package com.compiler.server

import com.compiler.server.base.BaseExecutorTest
import org.junit.jupiter.api.Test

class KotlinxSerialization: BaseExecutorTest() {
  @Test
  fun `encode object to string`() {
    run(
      code = """
        import kotlinx.serialization.*
        import kotlinx.serialization.json.*

        @Serializable 
        data class Project(val name: String, val language: String)

        fun main() {
          val project = Project("kotlinx.serialization", "Kotlin")
          println(Json.encodeToString(project)) 
        }
      """.trimIndent(),
      contains = """{"name":"kotlinx.serialization","language":"Kotlin"}"""
    )
  }

  @Test
  fun `decode object from JSON string`() {
    run(
      code = """
        import kotlinx.serialization.*
        import kotlinx.serialization.json.*

        @Serializable 
        data class Project(val name: String, val language: String)

        fun main() {
          val project = Project("kotlinx.serialization", "Kotlin")
          val obj = Json.decodeFromString<Project>(Json.encodeToString(project))
          println(obj)
        }
      """.trimIndent(),
      contains = "Project(name=kotlinx.serialization, language=Kotlin)"
    )
  }
}