package executors.streaming

import executors.synchronous.NO_MAIN_METHOD
import java.io.PrintStream
import java.lang.reflect.InvocationTargetException

class JavaStreamingRunnerExecutor {
  companion object {
    private val originalStdout = System.out
    private val outputMapper = StreamingOutputMapper()
    private val outStream = OutStreamStreaming(System.out, outputMapper)
    private val errStream = ErrorStreamStreaming(System.out, outputMapper)

    private fun closeStreams() {
      outStream.closeWrapper()
      errStream.closeWrapper()
    }

    @JvmStatic
    fun main(args: Array<String>) {
      try {
        System.setOut(PrintStream(outStream, true))
        System.setErr(PrintStream(errStream, true))

        val className: String
        if (args.isNotEmpty()) {
          className = args[0]
          try {
            val mainMethod = Class.forName(className)
              .getMethod("main", Array<String>::class.java)
            mainMethod.invoke(null, args.copyOfRange(1, args.size) as Any)
          }
          catch (e: InvocationTargetException) {
            closeStreams()
            val exception = e.cause ?: e
            originalStdout.write(outputMapper.writeThrowableAsBytes(exception))
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
      }
      catch (e: Throwable) {
        closeStreams()
        originalStdout.write(outputMapper.writeThrowableAsBytes(e))
      }
    }
  }
}