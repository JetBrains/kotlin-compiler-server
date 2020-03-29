package executors.synchronous

import executors.JUnitExecutorsUtil.loadTestClasses
import executors.TestListener
import executors.TestRunInfo
import executors.mapper
import org.junit.runner.JUnitCore
import org.junit.runner.Request
import java.util.*

class JUnitExecutor {
  companion object {
    private val output: MutableList<TestRunInfo> = ArrayList()
    private val standardOutput = System.out

    @JvmStatic
    fun main(args: Array<String>) {
      try {
        val jUnitCore = JUnitCore()
        jUnitCore.addListener(TestListener {
          output.add(it)
        })
        val cl = loadTestClasses(args[0])
        cl.forEach {
          val request = Request.aClass(it)
          jUnitCore.run(request)
        }
        System.setOut(standardOutput)
        val groupedTestResults = HashMap<String, MutableList<TestRunInfo>>()
        for (testRunInfo in output) {
          if (!groupedTestResults.containsKey(testRunInfo.className)) {
            groupedTestResults[testRunInfo.className] = ArrayList()
          }
          groupedTestResults[testRunInfo.className]?.add(testRunInfo)
        }
        print(mapper.writeValueAsString(groupedTestResults))
      }
      catch (e: Exception) {
        System.setOut(standardOutput)
        print("[\"")
        e.printStackTrace()
        print("\"]")
      }
    }
  }
}