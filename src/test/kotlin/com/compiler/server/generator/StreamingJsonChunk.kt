package com.compiler.server.generator

import com.compiler.server.model.ErrorDescriptor
import com.compiler.server.model.ExceptionDescriptor
import com.compiler.server.model.TestDescription
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlin.reflect.KClass

sealed class StreamingJsonChunk

data class OutStreamChunk(val outStream: String) : StreamingJsonChunk()
data class ErrStreamChunk(val errStream: String) : StreamingJsonChunk()
data class ErrorsChunk(val errors: Map<String, List<ErrorDescriptor>>) : StreamingJsonChunk()
data class ExceptionChunk(val exception: ExceptionDescriptor) : StreamingJsonChunk()
data class TestResultChunk(val testResult: TestDescription) : StreamingJsonChunk()
object UnknownChunk : StreamingJsonChunk()

object StreamingJsonChunkUtil {
  private val mapper = jacksonObjectMapper()

  fun readJsonChunk(value: String): StreamingJsonChunk =
      readOrNull(value, OutStreamChunk::class) ?:
      readOrNull(value, ErrStreamChunk::class) ?:
      readOrNull(value, ErrorsChunk::class) ?:
      readOrNull(value, ExceptionChunk::class) ?:
      readOrNull(value, TestResultChunk::class) ?:
      UnknownChunk


  private fun <T : Any> readOrNull(value: String, clazz: KClass<T>): T? =
      try {
        mapper.readValue(value, clazz.java)
      } catch (e: JsonProcessingException) {
        null
      }
}
