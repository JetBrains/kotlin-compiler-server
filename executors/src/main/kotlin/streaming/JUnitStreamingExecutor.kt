package executors.streaming

import executors.JUnitExecutorsUtil.loadTestClasses
import executors.TestListener
import executors.TestRunInfo
import executors.mapper
import org.junit.Test
import org.junit.runner.JUnitCore
import org.junit.runner.Request
import java.io.File
import java.util.*

class JUnitStreamingExecutor {
  companion object {
    private val outputMapper = StreamingOutputMapper()
    private val standardOutput = System.out

    @JvmStatic
    fun main(args: Array<String>) {
      try {
        val jUnitCore = JUnitCore()
        jUnitCore.addListener(TestListener {
          standardOutput.write(outputMapper.writeTestRunInfoAsBytes(it))
        })
        val cl = loadTestClasses(args[0])
        cl.forEach {
          val request = Request.aClass(it)
          jUnitCore.run(request)
        }
      }
      catch (e: Exception) {
        standardOutput.write(outputMapper.writeThrowableAsBytes(e))
      }
    }
  }
}