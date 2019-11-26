package executors


data class TestRunInfo(
  var output: String = "",
  val className: String = "",
  val methodName: String = "",
  var executionTime: Long = 0,
  var exception: Throwable? = null,
  var comparisonFailure: AssertionError? = null,
  var status: Status = Status.OK
)

enum class Status {
  OK,
  FAIL,
  ERROR
}