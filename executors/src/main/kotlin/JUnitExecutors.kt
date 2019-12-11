package executors

import org.junit.Test
import org.junit.runner.JUnitCore
import org.junit.runner.Request
import java.io.File
import java.util.*

class JUnitExecutors {
  companion object {
    var output: MutableList<TestRunInfo> = ArrayList()
    private val standardOutput = System.out

    @JvmStatic
    fun main(args: Array<String>) {
      try {
        val jUnitCore = JUnitCore()
        jUnitCore.addListener(TestListener())
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

    private fun loadTestClasses(path: String): List<Class<out Any>> {
      val files = File(path).listFiles().orEmpty()
      val names = files
        .filter { it.name.endsWith(".class") }
        .map { it.name.removeSuffix(".class") }
      return names.mapNotNull {
        try {
          Class.forName(it)
        }
        catch (_: Exception) {
          null
        }
      }.filter { it -> it.methods.any { it.isAnnotationPresent(Test::class.java) } }
    }
  }
}