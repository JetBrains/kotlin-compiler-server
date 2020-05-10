package com.compiler.server

import com.compiler.server.base.BaseExecutorTest
import com.compiler.server.generator.*
import com.compiler.server.generator.StreamingJsonChunkUtil.readJsonChunk
import com.compiler.server.model.TestStatus
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream

class StreamingSpecificTest : BaseExecutorTest() {
  
  @Test
  fun testStreamingExecutionWorks() {
    val outputStream = ByteArrayOutputStream() // it is thread-safe
    val project = generateSingleProject("""
      fun main() {
        print("stdout")
        System.out.flush() // just in case (but works without it)
        System.err.print("stderr")
        System.err.flush()
        Thread.sleep(Long.MAX_VALUE)
      }
    """.trimIndent())
    val projectExecutor = testRunner.kotlinProjectExecutor
    val executorThread = Thread { projectExecutor.runStreaming(project, outputStream) }
    executorThread.start()
    Thread.sleep(7000)
    val chunks = String(outputStream.toByteArray()).split("\n\n\n\n").map { readJsonChunk(it) }
    Assertions.assertEquals(3, chunks.size)
    Assertions.assertTrue(chunks[0] is ErrorsChunk, "First chunk should be ErrorsChunk")
    Assertions.assertTrue(chunks[1] is OutStreamChunk, "Second chunk should be OutStreamChunk")
    Assertions.assertEquals("stdout", (chunks[1] as OutStreamChunk).outStream)
    Assertions.assertTrue(chunks[2] is ErrStreamChunk, "Third chunk should be OutStreamChunk")
    Assertions.assertEquals("stderr", (chunks[2] as ErrStreamChunk).errStream)
    executorThread.join()
  }

  @Test
  fun testStreamingTestingWorks() {
    val outputStream = ByteArrayOutputStream() // it is thread-safe
    val project = generateSingleProject("""
      import org.junit.Assert
      import org.junit.FixMethodOrder
      import org.junit.runners.MethodSorters
      import org.junit.Test
      
      @FixMethodOrder(MethodSorters.NAME_ASCENDING)
      class StreamingTest {
        @Test  
        fun test1() {
           Assert.assertEquals(true, true)
        }
        
        @Test  
        fun test2() {
           Assert.assertEquals(true, false)
        }
        
        @Test
        fun test3() {
          Thread.sleep(Long.MAX_VALUE)
        }
      }
    """.trimIndent())
    val projectExecutor = testRunner.kotlinProjectExecutor
    val executorThread = Thread { projectExecutor.testStreaming(project, outputStream) }
    executorThread.start()
    Thread.sleep(7000)
    val chunks = String(outputStream.toByteArray()).split("\n\n\n\n").map { readJsonChunk(it) }
    Assertions.assertEquals(3, chunks.size)
    Assertions.assertTrue(chunks[0] is ErrorsChunk, "First chunk should be ErrorsChunk")
    Assertions.assertTrue(chunks[1] is TestResultChunk, "Second chunk should be OutStreamChunk")
    Assertions.assertEquals(TestStatus.OK, (chunks[1] as TestResultChunk).testResult.status)
    Assertions.assertTrue(chunks[2] is TestResultChunk, "Third chunk should be OutStreamChunk")
    Assertions.assertEquals(TestStatus.FAIL, (chunks[2] as TestResultChunk).testResult.status)
    executorThread.join()
  }
}