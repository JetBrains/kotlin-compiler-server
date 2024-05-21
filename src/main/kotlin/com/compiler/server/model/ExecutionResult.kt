package com.compiler.server.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize

open class ExecutionResult(
  @field:JsonProperty("errors")
  open var compilerDiagnostics: CompilerDiagnostics = CompilerDiagnostics(),
  open var exception: ExceptionDescriptor? = null
) {
  var text: String = ""
    set(value) {
      field = unEscapeOutput(value)
    }

  fun addWarnings(warnings: CompilerDiagnostics) {
    compilerDiagnostics = warnings
  }

  fun hasErrors() =
    textWithError() || exception != null || compilerDiagnostics.any { it.severity == ProjectSeveriry.ERROR }

  private fun textWithError() = text.startsWith(ERROR_STREAM_START)
}

class CompilerDiagnosticsSerializer : JsonSerializer<CompilerDiagnostics>() {
  override fun serialize(value: CompilerDiagnostics, gen: JsonGenerator, serializers: SerializerProvider) {
    gen.writeObject(value.map)
  }
}

class CompilerDiagnosticsDeserializer : JsonDeserializer<CompilerDiagnostics>() {
  private val reference = object : TypeReference<Map<String, List<ErrorDescriptor>>>() {}
  override fun deserialize(p: JsonParser, ctxt: DeserializationContext): CompilerDiagnostics {
    return p.readValueAs<Map<String, List<ErrorDescriptor>>>(reference).let(::CompilerDiagnostics)
  }
}

@JsonSerialize(using = CompilerDiagnosticsSerializer::class)
@JsonDeserialize(using = CompilerDiagnosticsDeserializer::class)
data class CompilerDiagnostics(
  val map: Map<String, List<ErrorDescriptor>> = mapOf()
): List<ErrorDescriptor> by map.values.flatten()

abstract class TranslationResultWithJsCode(
  open val jsCode: String?,
  compilerDiagnostics: CompilerDiagnostics,
  exception: ExceptionDescriptor?
) : ExecutionResult(compilerDiagnostics, exception)

data class TranslationJSResult(
  override val jsCode: String? = null,
  override var exception: ExceptionDescriptor? = null,
  @field:JsonProperty("errors")
  override var compilerDiagnostics: CompilerDiagnostics = CompilerDiagnostics()
) : TranslationResultWithJsCode(jsCode, compilerDiagnostics, exception)

data class TranslationWasmResult(
  override val jsCode: String? = null,
  val jsInstantiated: String,
  val wasm: ByteArray,
  val wat: String?,
  override var exception: ExceptionDescriptor? = null,
  @field:JsonProperty("errors")
  override var compilerDiagnostics: CompilerDiagnostics = CompilerDiagnostics()
) : TranslationResultWithJsCode(jsCode, compilerDiagnostics, exception)

@JsonInclude(JsonInclude.Include.NON_EMPTY)
class JunitExecutionResult(
  val testResults: Map<String, List<TestDescription>> = emptyMap(),
  override var exception: ExceptionDescriptor? = null,
  @field:JsonProperty("errors")
  override var compilerDiagnostics: CompilerDiagnostics = CompilerDiagnostics()
) : ExecutionResult(compilerDiagnostics, exception)

class SwiftExportResult(
  val swiftCode: String,
  override var exception: ExceptionDescriptor? = null,
  @field:JsonProperty("errors")
  override var compilerDiagnostics: CompilerDiagnostics = CompilerDiagnostics()
) : ExecutionResult(compilerDiagnostics, exception)


private fun unEscapeOutput(value: String) = value.replace("&amp;lt;".toRegex(), "<")
  .replace("&amp;gt;".toRegex(), ">")
  .replace("\r", "")
