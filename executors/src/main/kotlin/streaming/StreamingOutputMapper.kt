package executors.streaming

import executors.TestRunInfo
import executors.mapper
import java.io.ByteArrayOutputStream

open class StreamingOutputMapper {
  fun writeThrowableAsBytes(throwable: Throwable): ByteArray =
    writeCustomObjectAsBytes(throwable, EXCEPTION_FIELD_NAME)

  fun writeStdoutAsBytes(stdout: ByteArray): ByteArray = writeOutputAsBytes(stdout, STDOUT_FIELD_NAME)

  fun writeStdoutAsBytes(stdout: String): ByteArray = writeStdoutAsBytes(stdout.toByteArray())

  fun writeStderrAsBytes(stderr: ByteArray): ByteArray = writeOutputAsBytes(stderr, STDERR_FIELD_NAME)

  fun writeStderrAsBytes(stderr: String): ByteArray = writeStderrAsBytes(stderr.toByteArray())

  fun writeTestRunInfoAsBytes(testRunInfo: TestRunInfo): ByteArray =
    writeCustomObjectAsBytes(testRunInfo, TEST_RUN_INFO_FIELD_NAME)

  private fun writeOutputAsBytes(output: ByteArray, fieldName: String): ByteArray {
    val outputStream = ByteArrayOutputStream()
    mapper.factory.createGenerator(outputStream).use {
      it.writeStartObject()
      it.writeFieldName(fieldName)
      it.writeUTF8String(output, 0, output.size)
      it.writeEndObject()
    }
    return DELIMITER + outputStream.toByteArray() + DELIMITER
  }

  protected fun writeCustomObjectAsBytes(obj: Any, fieldName: String): ByteArray {
    val outputStream = ByteArrayOutputStream()
    mapper.factory.createGenerator(outputStream).use {
      it.writeStartObject()
      it.writeFieldName(fieldName)
      mapper.writeValue(it, obj)
      it.writeEndObject()
    }
    return DELIMITER + outputStream.toByteArray() + DELIMITER
  }

  companion object {
    private val DELIMITER = "\n\n".toByteArray()
    private const val EXCEPTION_FIELD_NAME = "exception"
    private const val STDOUT_FIELD_NAME = "outStream"
    private const val STDERR_FIELD_NAME = "errStream"
    private const val TEST_RUN_INFO_FIELD_NAME = "testResult"
  }
}