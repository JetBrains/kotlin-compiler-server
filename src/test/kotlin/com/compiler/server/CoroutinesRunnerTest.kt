package com.compiler.server

import com.compiler.server.base.BaseExecutorTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class CoroutinesRunnerTest : BaseExecutorTest() {

  @Test
  fun `base coroutines test 1`() {
    run(
      code = "import kotlinx.coroutines.*\n\nfun main() {\n    GlobalScope.launch { // launch a new coroutine in background and continue\n        delay(1000L) // non-blocking delay for 1 second (default time unit is ms)\n        println(\"World!\") // print after delay\n    }\n    println(\"Hello,\") // main thread continues while coroutine is delayed\n    Thread.sleep(2000L) // block main thread for 2 seconds to keep JVM alive\n}",
      contains = "Hello,\nWorld!\n"
    )
  }

  @Test
  fun `base coroutines test 2`() {
    run(
      code = "import kotlinx.coroutines.*\n\nfun main() { \n    GlobalScope.launch { // launch a new coroutine in background and continue\n        delay(1000L)\n        println(\"World!\")\n    }\n    println(\"Hello,\") // main thread continues here immediately\n    runBlocking {     // but this expression blocks the main thread\n        delay(2000L)  // ... while we delay for 2 seconds to keep JVM alive\n    } \n}",
      contains = "Hello,\nWorld!\n"
    )
  }

  @Test
  fun `base coroutines test 3`() {
    run(
      code = "import kotlinx.coroutines.*\n\nfun main() = runBlocking {\nval job = GlobalScope.launch { // launch a new coroutine and keep a reference to its Job\n    delay(1000L)\n    println(\"World!\")\n}\nprintln(\"Hello,\")\njob.join() // wait until child coroutine completes    \n}",
      contains = "Hello,\nWorld!\n"
    )
  }

  @Test
  fun `base coroutines test 4`() {
    run(
      code = "import kotlinx.coroutines.*\n\nfun main() = runBlocking { // this: CoroutineScope\n    launch { // launch a new coroutine in the scope of runBlocking\n        delay(1000L)\n        println(\"World!\")\n    }\n    println(\"Hello,\")\n}",
      contains = "Hello,\nWorld!\n"
    )
  }

  @Test
  fun `base coroutines test 5`() {
    run(
      code = "import kotlinx.coroutines.*\n\nfun main() = runBlocking { // this: CoroutineScope\n    launch { \n        delay(200L)\n        println(\"Task from runBlocking\")\n    }\n    \n    coroutineScope { // Creates a coroutine scope\n        launch {\n            delay(500L) \n            println(\"Task from nested launch\")\n        }\n    \n        delay(100L)\n        println(\"Task from coroutine scope\") // This line will be printed before the nested launch\n    }\n    \n    println(\"Coroutine scope is over\") // This line is not printed until the nested launch completes\n}",
      contains = "Task from coroutine scope\nTask from runBlocking\nTask from nested launch\nCoroutine scope is over\n"
    )
  }

  @Test
  fun `base coroutines test 6`() {
    run(
      code = "import kotlinx.coroutines.*\n\nfun main() = runBlocking {\n    launch { doWorld() }\n    println(\"Hello,\")\n}\n\n// this is your first suspending function\nsuspend fun doWorld() {\n    delay(1000L)\n    println(\"World!\")\n}",
      contains = "Hello,\nWorld!\n"
    )
  }

  @Test
  fun `base coroutines test 7`() {
    run(
      code = "import kotlinx.coroutines.*\n\nfun main() = runBlocking {\nGlobalScope.launch {\n    repeat(1000) { i ->\n        println(\"I'm sleeping \$i ...\")\n        delay(500L)\n    }\n}\ndelay(1300L) // just quit after delay    \n}",
      contains = "I'm sleeping 0 ...\nI'm sleeping 1 ...\nI'm sleeping 2 ...\n"
    )
  }

  @Test
  fun `base coroutines test 8`() {
    run(
      code = "import kotlinx.coroutines.*\n\nfun main() = runBlocking {\nval job = launch {\n    repeat(1000) { i ->\n        println(\"job: I'm sleeping \$i ...\")\n        delay(500L)\n    }\n}\ndelay(1300L) // delay a bit\nprintln(\"main: I'm tired of waiting!\")\njob.cancel() // cancels the job\njob.join() // waits for job's completion \nprintln(\"main: Now I can quit.\")    \n}",
      contains = "job: I'm sleeping 0 ...\njob: I'm sleeping 1 ...\njob: I'm sleeping 2 ...\nmain: I'm tired of waiting!\nmain: Now I can quit.\n"
    )
  }

  @Test
  fun `base coroutines test 9`() {
    run(
      code = "import kotlinx.coroutines.*\n\nfun main() = runBlocking {\nval startTime = System.currentTimeMillis()\nval job = launch(Dispatchers.Default) {\n    var nextPrintTime = startTime\n    var i = 0\n    while (i < 5) { // computation loop, just wastes CPU\n        // print a message twice a second\n        if (System.currentTimeMillis() >= nextPrintTime) {\n            println(\"job: I'm sleeping \${i++} ...\")\n            nextPrintTime += 500L\n        }\n    }\n}\ndelay(1300L) // delay a bit\nprintln(\"main: I'm tired of waiting!\")\njob.cancelAndJoin() // cancels the job and waits for its completion\nprintln(\"main: Now I can quit.\")    \n}",
      contains = "job: I'm sleeping 0 ...\njob: I'm sleeping 1 ...\njob: I'm sleeping 2 ...\nmain: I'm tired of waiting!\njob: I'm sleeping 3 ...\njob: I'm sleeping 4 ...\nmain: Now I can quit.\n"
    )
  }

  @Test
  fun `base coroutines test 10`() {
    run(
      code = "import kotlinx.coroutines.*\n\nfun main() = runBlocking {\nval startTime = System.currentTimeMillis()\nval job = launch(Dispatchers.Default) {\n    var nextPrintTime = startTime\n    var i = 0\n    while (isActive) { // cancellable computation loop\n        // print a message twice a second\n        if (System.currentTimeMillis() >= nextPrintTime) {\n            println(\"job: I'm sleeping \${i++} ...\")\n            nextPrintTime += 500L\n        }\n    }\n}\ndelay(1300L) // delay a bit\nprintln(\"main: I'm tired of waiting!\")\njob.cancelAndJoin() // cancels the job and waits for its completion\nprintln(\"main: Now I can quit.\")    \n}",
      contains = "job: I'm sleeping 0 ...\njob: I'm sleeping 1 ...\njob: I'm sleeping 2 ...\nmain: I'm tired of waiting!\nmain: Now I can quit.\n"
    )
  }

  @Test
  fun `base coroutines test 11`() {
    run(
      code = "import kotlinx.coroutines.*\n\nfun main() = runBlocking {\nval job = launch {\n    try {\n        repeat(1000) { i ->\n            println(\"job: I'm sleeping \$i ...\")\n            delay(500L)\n        }\n    } finally {\n        println(\"job: I'm running finally\")\n    }\n}\ndelay(1300L) // delay a bit\nprintln(\"main: I'm tired of waiting!\")\njob.cancelAndJoin() // cancels the job and waits for its completion\nprintln(\"main: Now I can quit.\")    \n}",
      contains = "job: I'm sleeping 0 ...\njob: I'm sleeping 1 ...\njob: I'm sleeping 2 ...\nmain: I'm tired of waiting!\njob: I'm running finally\nmain: Now I can quit.\n"
    )
  }

  @Test
  fun `base coroutines test 12`() {
    run(
      code = "import kotlinx.coroutines.*\n\nfun main() = runBlocking {\nval job = launch {\n    try {\n        repeat(1000) { i ->\n            println(\"job: I'm sleeping \$i ...\")\n            delay(500L)\n        }\n    } finally {\n        withContext(NonCancellable) {\n            println(\"job: I'm running finally\")\n            delay(1000L)\n            println(\"job: And I've just delayed for 1 sec because I'm non-cancellable\")\n        }\n    }\n}\ndelay(1300L) // delay a bit\nprintln(\"main: I'm tired of waiting!\")\njob.cancelAndJoin() // cancels the job and waits for its completion\nprintln(\"main: Now I can quit.\")    \n}",
      contains = "job: I'm sleeping 0 ...\njob: I'm sleeping 1 ...\njob: I'm sleeping 2 ...\nmain: I'm tired of waiting!\njob: I'm running finally\njob: And I've just delayed for 1 sec because I'm non-cancellable\nmain: Now I can quit.\n"
    )
  }

  @Test
  fun `base coroutines test 13`() {
    val expectedExMessage = "Timed out waiting for 1300 ms"
    val expectedEx = "kotlinx.coroutines.TimeoutCancellationException"
    val result = run(
      code = "import kotlinx.coroutines.*\n\nfun main() = runBlocking {\nwithTimeout(1300L) {\n    repeat(1000) { i ->\n        println(\"I'm sleeping \$i ...\")\n        delay(500L)\n    }\n}\n}",
      contains = "I'm sleeping 0 ...\nI'm sleeping 1 ...\nI'm sleeping 2 ...\n"
    )
    Assertions.assertTrue(
      result.exception?.message == expectedExMessage,
      "Actual: ${result.exception?.message}. Expected: $expectedExMessage"
    )
    Assertions.assertTrue(
      result.exception?.fullName == expectedEx,
      "Actual: ${result.exception?.fullName}. Expected: $expectedEx"
    )
  }

  @Test
  fun `base coroutines test 14`() {
    run(
      code = "import kotlinx.coroutines.*\n\nfun main() = runBlocking {\nval result = withTimeoutOrNull(1300L) {\n    repeat(1000) { i ->\n        println(\"I'm sleeping \$i ...\")\n        delay(500L)\n    }\n    \"Done\" // will get cancelled before it produces this result\n}\nprintln(\"Result is \$result\")\n}",
      contains = "I'm sleeping 0 ...\nI'm sleeping 1 ...\nI'm sleeping 2 ...\nResult is null\n"
    )
  }


  @Test
  fun `base coroutines test 15 Composing Suspending Functions`() {
    run(
      code = "import kotlinx.coroutines.*\nimport kotlin.system.*\n\nfun main() = runBlocking<Unit> {\nval time = measureTimeMillis {\n    val one = doSomethingUsefulOne()\n    val two = doSomethingUsefulTwo()\n    println(\"The answer is \${one + two}\")\n}\nprintln(\"Completed in \$time ms\")    \n}\n\nsuspend fun doSomethingUsefulOne(): Int {\n    delay(1000L) // pretend we are doing something useful here\n    return 13\n}\n\nsuspend fun doSomethingUsefulTwo(): Int {\n    delay(1000L) // pretend we are doing something useful here, too\n    return 29\n}",
      contains = "The answer is 42\nCompleted in"
    )
  }

  @Test
  fun `base coroutines test 16`() {
    run(
      code = "import kotlinx.coroutines.*\nimport kotlin.system.*\n\nfun main() = runBlocking<Unit> {\nval time = measureTimeMillis {\n    val one = async { doSomethingUsefulOne() }\n    val two = async { doSomethingUsefulTwo() }\n    println(\"The answer is \${one.await() + two.await()}\")\n}\nprintln(\"Completed in \$time ms\")    \n}\n\nsuspend fun doSomethingUsefulOne(): Int {\n    delay(1000L) // pretend we are doing something useful here\n    return 13\n}\n\nsuspend fun doSomethingUsefulTwo(): Int {\n    delay(1000L) // pretend we are doing something useful here, too\n    return 29\n}",
      contains = "The answer is 42\nCompleted in"
    )
  }

  @Test
  fun `base coroutines test 17`() {
    run(
      code = "import kotlinx.coroutines.*\nimport kotlin.system.*\n\nfun main() = runBlocking<Unit> {\nval time = measureTimeMillis {\n    val one = async(start = CoroutineStart.LAZY) { doSomethingUsefulOne() }\n    val two = async(start = CoroutineStart.LAZY) { doSomethingUsefulTwo() }\n    // some computation\n    one.start() // start the first one\n    two.start() // start the second one\n    println(\"The answer is \${one.await() + two.await()}\")\n}\nprintln(\"Completed in \$time ms\")    \n}\n\nsuspend fun doSomethingUsefulOne(): Int {\n    delay(1000L) // pretend we are doing something useful here\n    return 13\n}\n\nsuspend fun doSomethingUsefulTwo(): Int {\n    delay(1000L) // pretend we are doing something useful here, too\n    return 29\n}",
      contains = "The answer is 42\nCompleted in"
    )
  }

  @Test
  fun `base coroutines test 18`() {
    run(
      code = "import kotlinx.coroutines.*\nimport kotlin.system.*\n\n// note that we don't have `runBlocking` to the right of `main` in this example\nfun main() {\n    val time = measureTimeMillis {\n        // we can initiate async actions outside of a coroutine\n        val one = somethingUsefulOneAsync()\n        val two = somethingUsefulTwoAsync()\n        // but waiting for a result must involve either suspending or blocking.\n        // here we use `runBlocking { ... }` to block the main thread while waiting for the result\n        runBlocking {\n            println(\"The answer is \${one.await() + two.await()}\")\n        }\n    }\n    println(\"Completed in \$time ms\")\n}\n\nfun somethingUsefulOneAsync() = GlobalScope.async {\n    doSomethingUsefulOne()\n}\n\nfun somethingUsefulTwoAsync() = GlobalScope.async {\n    doSomethingUsefulTwo()\n}\n\nsuspend fun doSomethingUsefulOne(): Int {\n    delay(1000L) // pretend we are doing something useful here\n    return 13\n}\n\nsuspend fun doSomethingUsefulTwo(): Int {\n    delay(1000L) // pretend we are doing something useful here, too\n    return 29\n}",
      contains = "The answer is 42\nCompleted in"
    )
  }

  @Test
  fun `base coroutines test 19`() {
    run(
      code = "import kotlinx.coroutines.*\nimport kotlin.system.*\n\nfun main() = runBlocking<Unit> {\nval time = measureTimeMillis {\n    println(\"The answer is \${concurrentSum()}\")\n}\nprintln(\"Completed in \$time ms\")    \n}\n\nsuspend fun concurrentSum(): Int = coroutineScope {\n    val one = async { doSomethingUsefulOne() }\n    val two = async { doSomethingUsefulTwo() }\n    one.await() + two.await()\n}\n\nsuspend fun doSomethingUsefulOne(): Int {\n    delay(1000L) // pretend we are doing something useful here\n    return 13\n}\n\nsuspend fun doSomethingUsefulTwo(): Int {\n    delay(1000L) // pretend we are doing something useful here, too\n    return 29\n}",
      contains = "The answer is 42\nCompleted in"
    )
  }

  @Test
  fun `base coroutines test 20`() {
    run(
      code = "import kotlinx.coroutines.*\n\nfun main() = runBlocking<Unit> {\n    try {\n        failedConcurrentSum()\n    } catch(e: ArithmeticException) {\n        println(\"Computation failed with ArithmeticException\")\n    }\n}\n\nsuspend fun failedConcurrentSum(): Int = coroutineScope {\n    val one = async<Int> { \n        try {\n            delay(Long.MAX_VALUE) // Emulates very long computation\n            42\n        } finally {\n            println(\"First child was cancelled\")\n        }\n    }\n    val two = async<Int> { \n        println(\"Second child throws an exception\")\n        throw ArithmeticException()\n    }\n    one.await() + two.await()\n}",
      contains = "Second child throws an exception\nFirst child was cancelled\nComputation failed with ArithmeticException\n"
    )
  }


  @Test
  @Disabled
  fun `coroutines dispatchers & threads `() {
    run(
      code = "import kotlinx.coroutines.*\n\nfun main() = runBlocking<Unit> {\n    launch { // context of the parent, main runBlocking coroutine\n        println(\"main runBlocking      : I'm working in thread \${Thread.currentThread().name}\")\n    }\n    launch(Dispatchers.Unconfined) { // not confined -- will work with main thread\n        println(\"Unconfined            : I'm working in thread \${Thread.currentThread().name}\")\n    }\n    launch(Dispatchers.Default) { // will get dispatched to DefaultDispatcher \n        println(\"Default               : I'm working in thread \${Thread.currentThread().name}\")\n    }\n    launch(newSingleThreadContext(\"MyOwnThread\")) { // will get its own new thread\n        println(\"newSingleThreadContext: I'm working in thread \${Thread.currentThread().name}\")\n    }    \n}",
      contains = """
        Unconfined            : I'm working in thread main
        Default               : I'm working in thread DefaultDispatcher-worker-1
        newSingleThreadContext: I'm working in thread MyOwnThread
        main runBlocking      : I'm working in thread main
      """.trimIndent()
    )
  }


  @Test
  fun `base coroutines Unconfined vs confined dispatcher`() {
    run(
      code = "import kotlinx.coroutines.*\n\nfun main() = runBlocking<Unit> {\n    launch(Dispatchers.Unconfined) { // not confined -- will work with main thread\n        println(\"Unconfined      : I'm working in thread \${Thread.currentThread().name}\")\n        delay(500)\n        println(\"Unconfined      : After delay in thread \${Thread.currentThread().name}\")\n    }\n    launch { // context of the parent, main runBlocking coroutine\n        println(\"main runBlocking: I'm working in thread \${Thread.currentThread().name}\")\n        delay(1000)\n        println(\"main runBlocking: After delay in thread \${Thread.currentThread().name}\")\n    }    \n}",
      contains = """
        Unconfined      : I'm working in thread main @coroutine#2
        main runBlocking: I'm working in thread main @coroutine#3
        Unconfined      : After delay in thread kotlinx.coroutines.DefaultExecutor @coroutine#2
        main runBlocking: After delay in thread main @coroutine#3
      """.trimIndent()
    )
  }

  @Test
  fun `base coroutines Debugging coroutines and threads `() {
    run(
      code = "import kotlinx.coroutines.*\n\nfun log(msg: String) = println(\"[\${Thread.currentThread().name}] \$msg\")\n\nfun main() = runBlocking<Unit> {\nval a = async {\n    log(\"I'm computing a piece of the answer\")\n    6\n}\nval b = async {\n    log(\"I'm computing another piece of the answer\")\n    7\n}\nlog(\"The answer is \${a.await() * b.await()}\")    \n}",
      contains = """
        [main @coroutine#2] I'm computing a piece of the answer
        [main @coroutine#3] I'm computing another piece of the answer
        [main @coroutine#1] The answer is 42
      """.trimIndent()
    )
  }

  @Test
  fun `base coroutines test Jumping between threads`() {
    run(
      code = "import kotlinx.coroutines.*\n\nfun log(msg: String) = println(\"[\${Thread.currentThread().name}] \$msg\")\n\nfun main() {\nnewSingleThreadContext(\"Ctx1\").use { ctx1 ->\n    newSingleThreadContext(\"Ctx2\").use { ctx2 ->\n        runBlocking(ctx1) {\n            log(\"Started in ctx1\")\n            withContext(ctx2) {\n                log(\"Working in ctx2\")\n            }\n            log(\"Back to ctx1\")\n        }\n    }\n}    \n}",
      contains = """
        [Ctx1 @coroutine#1] Started in ctx1
        [Ctx2 @coroutine#1] Working in ctx2
        [Ctx1 @coroutine#1] Back to ctx1
      """.trimIndent()
    )
  }

  @Test
  fun `base coroutines test Job in the context`() {
    run(
      code = "import kotlinx.coroutines.*\n\nfun main() = runBlocking<Unit> {\nprintln(\"My job is \${coroutineContext[Job]}\")    \n}",
      contains = "My job is \"coroutine#1\":BlockingCoroutine{Active}"
    )
  }

  @Test
  fun `base coroutines test Children of a coroutine`() {
    run(
      code = "import kotlinx.coroutines.*\n\nfun main() = runBlocking<Unit> {\n// launch a coroutine to process some kind of incoming request\nval request = launch {\n    // it spawns two other jobs, one with GlobalScope\n    GlobalScope.launch {\n        println(\"job1: I run in GlobalScope and execute independently!\")\n        delay(1000)\n        println(\"job1: I am not affected by cancellation of the request\")\n    }\n    // and the other inherits the parent context\n    launch {\n        delay(100)\n        println(\"job2: I am a child of the request coroutine\")\n        delay(1000)\n        println(\"job2: I will not execute this line if my parent request is cancelled\")\n    }\n}\ndelay(500)\nrequest.cancel() // cancel processing of the request\ndelay(1000) // delay a second to see what happens\nprintln(\"main: Who has survived request cancellation?\")\n}",
      contains = """
        job1: I run in GlobalScope and execute independently!
        job2: I am a child of the request coroutine
        job1: I am not affected by cancellation of the request
        main: Who has survived request cancellation?
      """.trimIndent()
    )
  }

  @Test
  fun `base coroutines test Parental responsibilities`() {
    run(
      code = "import kotlinx.coroutines.*\n\nfun main() = runBlocking<Unit> {\n// launch a coroutine to process some kind of incoming request\nval request = launch {\n    repeat(3) { i -> // launch a few children jobs\n        launch  {\n            delay((i + 1) * 200L) // variable delay 200ms, 400ms, 600ms\n            println(\"Coroutine \$i is done\")\n        }\n    }\n    println(\"request: I'm done and I don't explicitly join my children that are still active\")\n}\nrequest.join() // wait for completion of the request, including all its children\nprintln(\"Now processing of the request is complete\")\n}",
      contains = """
        request: I'm done and I don't explicitly join my children that are still active
        Coroutine 0 is done
        Coroutine 1 is done
        Coroutine 2 is done
        Now processing of the request is complete
      """.trimIndent()
    )
  }


  @Test
  fun `base coroutines test `() {
    run(
      code = "",
      contains = ""
    )
  }

}