package com.compiler.server.base

import com.compiler.server.generator.ExceptionChunk
import com.compiler.server.generator.StreamingJsonChunk
import com.compiler.server.model.ExceptionDescriptor
import com.compiler.server.model.ExecutionResult


sealed class RawExecutionResult {
  abstract fun getException(): ExceptionDescriptor?
}

data class SynchronousResult(val result: ExecutionResult) : RawExecutionResult() {
  override fun getException(): ExceptionDescriptor? = result.exception
}

data class StreamingResult(val result: List<StreamingJsonChunk>) : RawExecutionResult() {
  override fun getException(): ExceptionDescriptor? =
      result.filterIsInstance(ExceptionChunk::class.java).firstOrNull()?.exception
}