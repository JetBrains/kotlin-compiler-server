package com.compiler.server.streaming

import com.compiler.server.model.ErrorDescriptor
import executors.streaming.StreamingOutputMapper
import org.springframework.stereotype.Component

@Component
class StreamingOutputMapperComponent: StreamingOutputMapper() {
  fun writeErrorsAsBytes(errors: Map<String, List<ErrorDescriptor>>): ByteArray =
    writeCustomObjectAsBytes(errors, "errors")
}