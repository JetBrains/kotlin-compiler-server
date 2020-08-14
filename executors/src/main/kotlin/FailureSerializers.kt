package executors

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import junit.framework.ComparisonFailure
import java.io.IOException
import kotlin.jvm.Throws

val mapper = ObjectMapper().apply {
  registerModule(SimpleModule().apply {
    addSerializer(Throwable::class.java, ThrowableSerializer())
    addSerializer(ComparisonFailure::class.java, JunitFrameworkComparisonFailureSerializer())
    addSerializer(org.junit.ComparisonFailure::class.java, OrgJunitComparisonFailureSerializer())
  })
}

class JunitFrameworkComparisonFailureSerializer : JsonSerializer<ComparisonFailure>() {
  @Throws(IOException::class)
  override fun serialize(value: ComparisonFailure, gen: JsonGenerator, serializers: SerializerProvider?) {
    gen.apply {
      writeStartObject()
      writeStringField("message", value.message)
      writeStringField("expected", value.expected)
      writeStringField("actual", value.actual)
      writeStringField("fullName", value.javaClass.name)
      writeObjectField("stackTrace", value.stackTrace)
      writeObjectField("cause", if (value.cause != value) value.cause else null)
      writeEndObject()
    }
  }
}

class OrgJunitComparisonFailureSerializer : JsonSerializer<org.junit.ComparisonFailure>() {
  @Throws(IOException::class)
  override fun serialize(value: org.junit.ComparisonFailure, gen: JsonGenerator, serializers: SerializerProvider?) {
    gen.apply {
      writeStartObject()
      writeStringField("message", value.message)
      writeStringField("expected", value.expected)
      writeStringField("actual", value.actual)
      writeStringField("fullName", value.javaClass.name)
      writeObjectField("stackTrace", value.stackTrace)
      writeObjectField("cause", if (value.cause !== value) value.cause else null)
      writeEndObject()
    }
  }
}

class ThrowableSerializer : JsonSerializer<Throwable>() {
  @Throws(IOException::class)
  override fun serialize(value: Throwable, gen: JsonGenerator, serializers: SerializerProvider) {
    gen.writeStartObject()
    gen.writeStringField("message", value.message)
    gen.writeStringField("fullName", value.javaClass.name)
    gen.writeObjectField("stackTrace", value.stackTrace?.take(3))
    gen.writeObjectField("cause", if (value.cause != value) value.cause else null)
    gen.writeEndObject()
  }
}
