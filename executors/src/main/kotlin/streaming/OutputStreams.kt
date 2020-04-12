package executors.streaming

import java.io.IOException
import java.io.OutputStream

internal abstract class BaseStreamingWrapper : OutputStream() {
  private var isOpen = true

  @Throws(IOException::class)
  @Synchronized
  final override fun write(b: Int) {
    if (isOpen) {
      writeToStream(byteArrayOf(b.toByte()))
    }
  }

  @Throws(IOException::class)
  @Synchronized
  final override fun write(b: ByteArray) {
    if (isOpen) {
      writeToStream(b)
    }
  }

  @Throws(IOException::class)
  @Synchronized
  final override fun write(b: ByteArray, offset: Int, length: Int) {
    if (isOpen) {
      writeToStream(b.copyOfRange(offset, offset + length))
    }
  }

  @Synchronized
  fun closeWrapper() {
    isOpen = false
  }

  abstract fun writeToStream(b: ByteArray)
}

internal class ErrorStreamStreaming(
  private val outputStream: OutputStream,
  private val outputMapper: StreamingOutputMapper
) : BaseStreamingWrapper() {

  override fun writeToStream(b: ByteArray) {
    outputStream.write(outputMapper.writeStderrAsBytes(b))
  }
}

internal class OutStreamStreaming(
  private val outputStream: OutputStream,
  private val outputMapper: StreamingOutputMapper
) : BaseStreamingWrapper() {

  override fun writeToStream(b: ByteArray) {
    outputStream.write(outputMapper.writeStdoutAsBytes(b))
  }
}