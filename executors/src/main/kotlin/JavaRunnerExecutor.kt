package executors

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.lang.reflect.InvocationTargetException

const val NO_MAIN_METHOD = "No main method found in project."

class JavaRunnerExecutor {
  companion object {
    private val outputStream = ByteArrayOutputStream()
    private val errorOutputStream = ErrorStream(outputStream)
    private val standardOutputStream = OutStream(outputStream)
    @JvmStatic
    fun main(args: Array<String>) {
      val defaultOutputStream = System.out
      try {
        System.setOut(PrintStream(standardOutputStream))
        System.setErr(PrintStream(errorOutputStream))
        val outputObj = RunOutput()
        val className: String
        if (args.isNotEmpty()) {
          className = args[0]
          try {
            val mainMethod = Class.forName(className)
              .getMethod("main", Array<String>::class.java)
            mainMethod.invoke(null, args.copyOfRange(1, args.size) as Any)
          }
          catch (e: InvocationTargetException) {
            outputObj.exception = e.cause
          }
          catch (e: NoSuchMethodException) {
            System.err.println(NO_MAIN_METHOD)
          }
          catch (e: ClassNotFoundException) {
            System.err.println(NO_MAIN_METHOD)
          }
        }
        else {
          System.err.println(NO_MAIN_METHOD)
        }
        System.out.flush()
        System.err.flush()
        outputObj.text = outputStream.toString()
          .replace("</errStream><errStream>".toRegex(), "")
          .replace("</outStream><outStream>".toRegex(), "")
        defaultOutputStream.print(mapper.writeValueAsString(outputObj))
      }
      catch (e: Throwable) {
        System.setOut(defaultOutputStream)
        println(mapper.writeValueAsString(RunOutput(exception = e)))
      }
    }
  }
}

data class RunOutput(var text: String = "", var exception: Throwable? = null)
