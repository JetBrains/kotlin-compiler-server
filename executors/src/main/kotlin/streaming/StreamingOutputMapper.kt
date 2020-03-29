package executors.streaming

import executors.TestRunInfo
import executors.mapper
import java.io.ByteArrayOutputStream

open class StreamingOutputMapper {
  fun writeThrowableAsBytes(throwable: Throwable): ByteArray =
    writeCustomObjectAsBytes(throwable, "exception")

  fun writeStdoutAsBytes(stdout: ByteArray): ByteArray = writeOutputAsBytes(stdout, "outStream")

  fun writeStdoutAsBytes(stdout: String): ByteArray = writeStdoutAsBytes(stdout.toByteArray())

  fun writeStderrAsBytes(stderr: ByteArray): ByteArray = writeOutputAsBytes(stderr, "errStream")

  fun writeStderrAsBytes(stderr: String): ByteArray = writeStderrAsBytes(stderr.toByteArray())

  fun writeTestRunInfoAsBytes(testRunInfo: TestRunInfo): ByteArray =
    writeCustomObjectAsBytes(testRunInfo, "testResult")

  private fun writeOutputAsBytes(output: ByteArray, fieldName: String): ByteArray {
    val outputStream = ByteArrayOutputStream()
    val generator = mapper.factory.createGenerator(outputStream)
    generator.writeStartObject()
    generator.writeFieldName(fieldName)
    generator.writeUTF8String(output, 0, output.size)
    generator.writeEndObject()
    generator.close()
    return DELIMITER + outputStream.toByteArray()
  }

  protected fun writeCustomObjectAsBytes(obj: Any, fieldName: String): ByteArray {
    val outputStream = ByteArrayOutputStream()
    val generator = mapper.factory.createGenerator(outputStream)
    generator.writeStartObject()
    generator.writeFieldName(fieldName)
    mapper.writeValue(generator, obj)
    generator.writeEndObject()
    generator.close()
    return DELIMITER + outputStream.toByteArray()
  }

  companion object {
    private val DELIMITER = "\n\n".toByteArray()
  }
}