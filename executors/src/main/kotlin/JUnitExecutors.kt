package executors

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import junit.framework.ComparisonFailure
import org.junit.Test
import org.junit.runner.JUnitCore
import org.junit.runner.Request
import java.io.File
import java.util.*

class JUnitExecutors {
  companion object {
    var output: MutableList<TestRunInfo> = ArrayList()
    private val standardOutput = System.out
    private val mapper = ObjectMapper().apply {
      registerModule(SimpleModule().apply {
        addSerializer(Throwable::class.java, ThrowableSerializer())
        addSerializer(ComparisonFailure::class.java, JunitFrameworkComparisonFailureSerializer())
        addSerializer(org.junit.ComparisonFailure::class.java, OrgJunitComparisonFailureSerializer())
      })
    }

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
        print(e)
        System.setOut(standardOutput)
        e.printStackTrace()
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