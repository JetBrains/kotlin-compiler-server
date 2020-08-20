package executors

import org.junit.runner.Description
import org.junit.runner.notification.Failure
import org.junit.runner.notification.RunListener
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.io.PrintStream

internal class TestListener : RunListener() {
  private var startTime: Long = 0
  private val ignoreStream = PrintStream(object : OutputStream() {
    @Throws(IOException::class)
    override fun write(b: Int) {
    }
  })
  private var testOutputStream: ByteArrayOutputStream? = null
  private var errorStream: ErrorStream? = null
  private var outStream: OutStream? = null
  private var currentTestRunInfo: TestRunInfo? = null

  override fun testStarted(description: Description) {
    currentTestRunInfo = TestRunInfo(
      className = description.className,
      methodName = description.methodName
    )
    currentTestRunInfo?.let { JUnitExecutors.output.add(it) }
    testOutputStream = ByteArrayOutputStream().also {
      errorStream = ErrorStream(it)
      outStream = OutStream(it)
    }
    System.setOut(PrintStream(outStream))
    System.setErr(PrintStream(errorStream))
    startTime = System.currentTimeMillis()
  }

  override fun testFailure(failure: Failure) {
    val failureException = failure.exception
    when (failureException) {
      is AssertionError -> currentTestRunInfo?.apply {
        status = Status.FAIL
        comparisonFailure = failureException
      }
      else -> currentTestRunInfo?.apply {
        status = Status.ERROR
        exception = failureException
      }
    }
  }

  override fun testFinished(description: Description?) {
    System.out.flush()
    System.err.flush()
    JUnitExecutors.output[JUnitExecutors.output.size - 1].apply {
      executionTime = System.currentTimeMillis() - startTime
      output = testOutputStream?.toString().orEmpty()
        .replace("</errStream><errStream>".toRegex(), "")
        .replace("</outStream><outStream>".toRegex(), "")
    }
    System.setOut(ignoreStream)
  }
}