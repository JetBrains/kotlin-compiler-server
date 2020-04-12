package com.compiler.server.base

import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.*
import java.util.stream.Stream

abstract class BaseTest {
  @Volatile
  lateinit var mode: ExecutorMode
}

enum class ExecutorMode {
  SYNCHRONOUS,
  STREAMING
}

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@TestTemplate
@ExtendWith(BothModesContextProvider::class)
annotation class TestCompiler

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@TestTemplate
@ExtendWith(StreamingContextProvider::class)
annotation class TestStreaming

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@TestTemplate
@ExtendWith(SynchronousContextProvider::class)
annotation class TestSync

private class StreamingContextProvider : ExecutorModeInvocationContextProvider(listOf(ExecutorMode.STREAMING))
private class SynchronousContextProvider : ExecutorModeInvocationContextProvider(listOf(ExecutorMode.SYNCHRONOUS))
private class BothModesContextProvider :
    ExecutorModeInvocationContextProvider(listOf(ExecutorMode.SYNCHRONOUS, ExecutorMode.STREAMING))

private abstract class ExecutorModeInvocationContextProvider(
    private val modes: List<ExecutorMode>
) : TestTemplateInvocationContextProvider {

  override fun provideTestTemplateInvocationContexts(
      context: ExtensionContext
  ): Stream<TestTemplateInvocationContext> = modes.map {
    val invocationContext: TestTemplateInvocationContext = ExecutorModeInvocationContext(it)
    invocationContext // java Stream is invariant
  }.stream()

  override fun supportsTestTemplate(context: ExtensionContext): Boolean =
      BaseTest::class.java.isAssignableFrom(context.requiredTestClass)
}

private class ExecutorModeInvocationContext(private val mode: ExecutorMode) : TestTemplateInvocationContext {
  override fun getDisplayName(invocationIndex: Int): String = mode.name

  override fun getAdditionalExtensions(): List<Extension> = listOf(
      object : TestInstancePostProcessor {
        override fun postProcessTestInstance(testInstance: Any, context: ExtensionContext) {
          (testInstance as BaseTest).mode = mode
        }
      }
  )
}