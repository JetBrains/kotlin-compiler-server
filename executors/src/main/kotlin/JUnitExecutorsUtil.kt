package executors

import org.junit.Test
import java.io.File

object JUnitExecutorsUtil {
  fun loadTestClasses(path: String): List<Class<out Any>> {
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