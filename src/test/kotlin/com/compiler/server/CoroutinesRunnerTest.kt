package com.compiler.server

import com.compiler.server.generator.TestProjectRunner
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class CoroutinesRunnerTest {
  @Autowired
  private lateinit var testRunner: TestProjectRunner

  @Test
  fun `base coroutines test 1`() {
    testRunner.run(
      code = "import kotlinx.coroutines.*\n\nfun main() {\n    GlobalScope.launch { // launch a new coroutine in background and continue\n        delay(1000L) // non-blocking delay for 1 second (default time unit is ms)\n        println(\"World!\") // print after delay\n    }\n    println(\"Hello,\") // main thread continues while coroutine is delayed\n    Thread.sleep(2000L) // block main thread for 2 seconds to keep JVM alive\n}",
      contains = "Hello,\nWorld!\n"
    )
  }

  @Test
  fun `base coroutines test 2`() {
    testRunner.run(
      code = "import kotlinx.coroutines.*\n\nfun main() { \n    GlobalScope.launch { // launch a new coroutine in background and continue\n        delay(1000L)\n        println(\"World!\")\n    }\n    println(\"Hello,\") // main thread continues here immediately\n    runBlocking {     // but this expression blocks the main thread\n        delay(2000L)  // ... while we delay for 2 seconds to keep JVM alive\n    } \n}",
      contains = "Hello,\nWorld!\n"
    )
  }

  @Test
  fun `base coroutines test 3`() {
    testRunner.run(
      code = "import kotlinx.coroutines.*\n\nfun main() = runBlocking {\nval job = GlobalScope.launch { // launch a new coroutine and keep a reference to its Job\n    delay(1000L)\n    println(\"World!\")\n}\nprintln(\"Hello,\")\njob.join() // wait until child coroutine completes    \n}",
      contains = "Hello,\nWorld!\n"
    )
  }

  @Test
  fun `base coroutines test 4`() {
    testRunner.run(
      code = "import kotlinx.coroutines.*\n\nfun main() = runBlocking { // this: CoroutineScope\n    launch { // launch a new coroutine in the scope of runBlocking\n        delay(1000L)\n        println(\"World!\")\n    }\n    println(\"Hello,\")\n}",
      contains = "Hello,\nWorld!\n"
    )
  }

  @Test
  fun `base coroutines test 5`() {
    testRunner.run(
      code = "import kotlinx.coroutines.*\n\nfun main() = runBlocking { // this: CoroutineScope\n    launch { \n        delay(200L)\n        println(\"Task from runBlocking\")\n    }\n    \n    coroutineScope { // Creates a coroutine scope\n        launch {\n            delay(500L) \n            println(\"Task from nested launch\")\n        }\n    \n        delay(100L)\n        println(\"Task from coroutine scope\") // This line will be printed before the nested launch\n    }\n    \n    println(\"Coroutine scope is over\") // This line is not printed until the nested launch completes\n}",
      contains = "Task from coroutine scope\nTask from runBlocking\nTask from nested launch\nCoroutine scope is over\n"
    )
  }

  @Test
  fun `base coroutines test 6`() {
    testRunner.run(
      code = "import kotlinx.coroutines.*\n\nfun main() = runBlocking {\n    launch { doWorld() }\n    println(\"Hello,\")\n}\n\n// this is your first suspending function\nsuspend fun doWorld() {\n    delay(1000L)\n    println(\"World!\")\n}",
      contains = "Hello,\nWorld!\n"
    )
  }

  @Test
  fun `base coroutines test 7`() {
    testRunner.run(
      code = "import kotlinx.coroutines.*\n\nfun main() = runBlocking {\nGlobalScope.launch {\n    repeat(1000) { i ->\n        println(\"I'm sleeping \$i ...\")\n        delay(500L)\n    }\n}\ndelay(1300L) // just quit after delay    \n}",
      contains = "I'm sleeping 0 ...\nI'm sleeping 1 ...\nI'm sleeping 2 ...\n"
    )
  }

  @Test
  fun `base coroutines test 8`() {
    testRunner.run(
      code = "import kotlinx.coroutines.*\n\nfun main() = runBlocking {\nval job = launch {\n    repeat(1000) { i ->\n        println(\"job: I'm sleeping \$i ...\")\n        delay(500L)\n    }\n}\ndelay(1300L) // delay a bit\nprintln(\"main: I'm tired of waiting!\")\njob.cancel() // cancels the job\njob.join() // waits for job's completion \nprintln(\"main: Now I can quit.\")    \n}",
      contains = "job: I'm sleeping 0 ...\njob: I'm sleeping 1 ...\njob: I'm sleeping 2 ...\nmain: I'm tired of waiting!\nmain: Now I can quit.\n"
    )
  }

  @Test
  fun `base coroutines test 9`() {
    testRunner.run(
      code = "import kotlinx.coroutines.*\n\nfun main() = runBlocking {\nval startTime = System.currentTimeMillis()\nval job = launch(Dispatchers.Default) {\n    var nextPrintTime = startTime\n    var i = 0\n    while (i < 5) { // computation loop, just wastes CPU\n        // print a message twice a second\n        if (System.currentTimeMillis() >= nextPrintTime) {\n            println(\"job: I'm sleeping \${i++} ...\")\n            nextPrintTime += 500L\n        }\n    }\n}\ndelay(1300L) // delay a bit\nprintln(\"main: I'm tired of waiting!\")\njob.cancelAndJoin() // cancels the job and waits for its completion\nprintln(\"main: Now I can quit.\")    \n}",
      contains = "job: I'm sleeping 0 ...\njob: I'm sleeping 1 ...\njob: I'm sleeping 2 ...\nmain: I'm tired of waiting!\njob: I'm sleeping 3 ...\njob: I'm sleeping 4 ...\nmain: Now I can quit.\n"
    )
  }

  @Test
  fun `base coroutines test 10`() {
    testRunner.run(
      code = "import kotlinx.coroutines.*\n\nfun main() = runBlocking {\nval startTime = System.currentTimeMillis()\nval job = launch(Dispatchers.Default) {\n    var nextPrintTime = startTime\n    var i = 0\n    while (isActive) { // cancellable computation loop\n        // print a message twice a second\n        if (System.currentTimeMillis() >= nextPrintTime) {\n            println(\"job: I'm sleeping \${i++} ...\")\n            nextPrintTime += 500L\n        }\n    }\n}\ndelay(1300L) // delay a bit\nprintln(\"main: I'm tired of waiting!\")\njob.cancelAndJoin() // cancels the job and waits for its completion\nprintln(\"main: Now I can quit.\")    \n}",
      contains = "job: I'm sleeping 0 ...\njob: I'm sleeping 1 ...\njob: I'm sleeping 2 ...\nmain: I'm tired of waiting!\nmain: Now I can quit.\n"
    )
  }

  @Test
  fun `base coroutines test 11`() {
    testRunner.run(
      code = "import kotlinx.coroutines.*\n\nfun main() = runBlocking {\nval job = launch {\n    try {\n        repeat(1000) { i ->\n            println(\"job: I'm sleeping \$i ...\")\n            delay(500L)\n        }\n    } finally {\n        println(\"job: I'm running finally\")\n    }\n}\ndelay(1300L) // delay a bit\nprintln(\"main: I'm tired of waiting!\")\njob.cancelAndJoin() // cancels the job and waits for its completion\nprintln(\"main: Now I can quit.\")    \n}",
      contains = "job: I'm sleeping 0 ...\njob: I'm sleeping 1 ...\njob: I'm sleeping 2 ...\nmain: I'm tired of waiting!\njob: I'm running finally\nmain: Now I can quit.\n"
    )
  }

  @Test
  fun `base coroutines test 12`() {
    testRunner.run(
      code = "import kotlinx.coroutines.*\n\nfun main() = runBlocking {\nval job = launch {\n    try {\n        repeat(1000) { i ->\n            println(\"job: I'm sleeping \$i ...\")\n            delay(500L)\n        }\n    } finally {\n        withContext(NonCancellable) {\n            println(\"job: I'm running finally\")\n            delay(1000L)\n            println(\"job: And I've just delayed for 1 sec because I'm non-cancellable\")\n        }\n    }\n}\ndelay(1300L) // delay a bit\nprintln(\"main: I'm tired of waiting!\")\njob.cancelAndJoin() // cancels the job and waits for its completion\nprintln(\"main: Now I can quit.\")    \n}",
      contains = "job: I'm sleeping 0 ...\njob: I'm sleeping 1 ...\njob: I'm sleeping 2 ...\nmain: I'm tired of waiting!\njob: I'm running finally\njob: And I've just delayed for 1 sec because I'm non-cancellable\nmain: Now I can quit.\n"
    )
  }

  @Test
  fun `base coroutines test 13`() {
    val expectedExMessage = "Timed out waiting for 1300 ms"
    val expectedEx = "kotlinx.coroutines.TimeoutCancellationException"
    val result = testRunner.run(
      code = "import kotlinx.coroutines.*\n\nfun main() = runBlocking {\nwithTimeout(1300L) {\n    repeat(1000) { i ->\n        println(\"I'm sleeping \$i ...\")\n        delay(500L)\n    }\n}\n}",
      contains = "I'm sleeping 0 ...\nI'm sleeping 1 ...\nI'm sleeping 2 ...\n"
    )
    Assertions.assertTrue(result.exception?.message == expectedExMessage,
                          "Actual: ${result.exception?.message}. Expected: $expectedExMessage")
    Assertions.assertTrue(result.exception?.fullName == expectedEx, "Actual: ${result.exception?.fullName}. Expected: $expectedEx")
  }

  @Test
  fun `base coroutines test 14`() {
    testRunner.run(
      code = "import kotlinx.coroutines.*\n\nfun main() = runBlocking {\nval result = withTimeoutOrNull(1300L) {\n    repeat(1000) { i ->\n        println(\"I'm sleeping \$i ...\")\n        delay(500L)\n    }\n    \"Done\" // will get cancelled before it produces this result\n}\nprintln(\"Result is \$result\")\n}",
      contains = "I'm sleeping 0 ...\nI'm sleeping 1 ...\nI'm sleeping 2 ...\nResult is null\n"
    )
  }


  @Test
  fun `base coroutines test 15 Composing Suspending Functions`() {
    testRunner.run(
      code = "import kotlinx.coroutines.*\nimport kotlin.system.*\n\nfun main() = runBlocking<Unit> {\nval time = measureTimeMillis {\n    val one = doSomethingUsefulOne()\n    val two = doSomethingUsefulTwo()\n    println(\"The answer is \${one + two}\")\n}\nprintln(\"Completed in \$time ms\")    \n}\n\nsuspend fun doSomethingUsefulOne(): Int {\n    delay(1000L) // pretend we are doing something useful here\n    return 13\n}\n\nsuspend fun doSomethingUsefulTwo(): Int {\n    delay(1000L) // pretend we are doing something useful here, too\n    return 29\n}",
      contains = "The answer is 42\nCompleted in"
    )
  }

  @Test
  fun `base coroutines test 16`() {
    testRunner.run(
      code = "import kotlinx.coroutines.*\nimport kotlin.system.*\n\nfun main() = runBlocking<Unit> {\nval time = measureTimeMillis {\n    val one = async { doSomethingUsefulOne() }\n    val two = async { doSomethingUsefulTwo() }\n    println(\"The answer is \${one.await() + two.await()}\")\n}\nprintln(\"Completed in \$time ms\")    \n}\n\nsuspend fun doSomethingUsefulOne(): Int {\n    delay(1000L) // pretend we are doing something useful here\n    return 13\n}\n\nsuspend fun doSomethingUsefulTwo(): Int {\n    delay(1000L) // pretend we are doing something useful here, too\n    return 29\n}",
      contains = "The answer is 42\nCompleted in"
    )
  }

  @Test
  fun `base coroutines test 17`() {
    testRunner.run(
      code = "import kotlinx.coroutines.*\nimport kotlin.system.*\n\nfun main() = runBlocking<Unit> {\nval time = measureTimeMillis {\n    val one = async(start = CoroutineStart.LAZY) { doSomethingUsefulOne() }\n    val two = async(start = CoroutineStart.LAZY) { doSomethingUsefulTwo() }\n    // some computation\n    one.start() // start the first one\n    two.start() // start the second one\n    println(\"The answer is \${one.await() + two.await()}\")\n}\nprintln(\"Completed in \$time ms\")    \n}\n\nsuspend fun doSomethingUsefulOne(): Int {\n    delay(1000L) // pretend we are doing something useful here\n    return 13\n}\n\nsuspend fun doSomethingUsefulTwo(): Int {\n    delay(1000L) // pretend we are doing something useful here, too\n    return 29\n}",
      contains = "The answer is 42\nCompleted in"
    )
  }

  @Test
  fun `base coroutines test 18`() {
    testRunner.run(
      code = "import kotlinx.coroutines.*\nimport kotlin.system.*\n\n// note that we don't have `runBlocking` to the right of `main` in this example\nfun main() {\n    val time = measureTimeMillis {\n        // we can initiate async actions outside of a coroutine\n        val one = somethingUsefulOneAsync()\n        val two = somethingUsefulTwoAsync()\n        // but waiting for a result must involve either suspending or blocking.\n        // here we use `runBlocking { ... }` to block the main thread while waiting for the result\n        runBlocking {\n            println(\"The answer is \${one.await() + two.await()}\")\n        }\n    }\n    println(\"Completed in \$time ms\")\n}\n\nfun somethingUsefulOneAsync() = GlobalScope.async {\n    doSomethingUsefulOne()\n}\n\nfun somethingUsefulTwoAsync() = GlobalScope.async {\n    doSomethingUsefulTwo()\n}\n\nsuspend fun doSomethingUsefulOne(): Int {\n    delay(1000L) // pretend we are doing something useful here\n    return 13\n}\n\nsuspend fun doSomethingUsefulTwo(): Int {\n    delay(1000L) // pretend we are doing something useful here, too\n    return 29\n}",
      contains = "The answer is 42\nCompleted in"
    )
  }

  @Test
  fun `base coroutines test 19`() {
    testRunner.run(
      code = "import kotlinx.coroutines.*\nimport kotlin.system.*\n\nfun main() = runBlocking<Unit> {\nval time = measureTimeMillis {\n    println(\"The answer is \${concurrentSum()}\")\n}\nprintln(\"Completed in \$time ms\")    \n}\n\nsuspend fun concurrentSum(): Int = coroutineScope {\n    val one = async { doSomethingUsefulOne() }\n    val two = async { doSomethingUsefulTwo() }\n    one.await() + two.await()\n}\n\nsuspend fun doSomethingUsefulOne(): Int {\n    delay(1000L) // pretend we are doing something useful here\n    return 13\n}\n\nsuspend fun doSomethingUsefulTwo(): Int {\n    delay(1000L) // pretend we are doing something useful here, too\n    return 29\n}",
      contains = "The answer is 42\nCompleted in"
    )
  }

  @Test
  fun `base coroutines test 20`() {
    testRunner.run(
      code = "import kotlinx.coroutines.*\n\nfun main() = runBlocking<Unit> {\n    try {\n        failedConcurrentSum()\n    } catch(e: ArithmeticException) {\n        println(\"Computation failed with ArithmeticException\")\n    }\n}\n\nsuspend fun failedConcurrentSum(): Int = coroutineScope {\n    val one = async<Int> { \n        try {\n            delay(Long.MAX_VALUE) // Emulates very long computation\n            42\n        } finally {\n            println(\"First child was cancelled\")\n        }\n    }\n    val two = async<Int> { \n        println(\"Second child throws an exception\")\n        throw ArithmeticException()\n    }\n    one.await() + two.await()\n}",
      contains = "Second child throws an exception\nFirst child was cancelled\nComputation failed with ArithmeticException\n"
    )
  }


  @Test
  fun `base coroutines test `() {
    testRunner.run(
      code = "",
      contains = ""
    )
  }

}