package com.compiler.server.streaming

import com.compiler.server.model.ErrorDescriptor
import executors.streaming.StreamingOutputMapper

class ServerStreamingOutputMapper: StreamingOutputMapper() {
  fun writeErrorsAsBytes(errors: Map<String, List<ErrorDescriptor>>): ByteArray =
    writeCustomObjectAsBytes(errors, ERRORS_FIELD_NAME)

  companion object {
    private const val ERRORS_FIELD_NAME = "errors"
  }
}