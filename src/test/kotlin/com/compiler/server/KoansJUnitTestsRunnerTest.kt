package com.compiler.server

import com.compiler.server.base.BaseJUnitTest
import com.compiler.server.base.ExecutorMode
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class KoansJUnitTestsRunnerTest : BaseJUnitTest() {

  @ParameterizedTest
  @EnumSource(ExecutorMode::class)
  fun `base junit test`(mode: ExecutorMode) {
    runKoanTest(
      mode,
      "fun start(): String = \"OK\"",
      "import org.junit.Assert\nimport org.junit.Test\n\nclass TestStart {\n    @Test fun testOk() {\n        Assert.assertEquals(\"OK\", start())\n    }\n}"
    )
  }

  @ParameterizedTest
  @EnumSource(ExecutorMode::class)
  fun `koans test Named arguments`(mode: ExecutorMode) {
    runKoanTest(
      mode,
      "fun joinOptions(options: Collection<String>) = options.joinToString(prefix = \"[\", postfix = \"]\")",
      "import org.junit.Test\nimport org.junit.Assert\nimport koans.util.toMessageInEquals\n\nclass TestNamedArguments() {\n\n    @Test fun testJoinToString() {\n        Assert.assertEquals(\"joinOptions\".toMessageInEquals(), \"[yes, no, may be]\", joinOptions(listOf(\"yes\", \"no\", \"may be\")))\n    }\n\n}"
    )
  }

  @ParameterizedTest
  @EnumSource(ExecutorMode::class)
  fun `koans test Default arguments`(mode: ExecutorMode) {
    runKoanTest(
      mode,
      "fun foo(name: String, number: Int = 42, toUpperCase: Boolean = false) =\n        (if (toUpperCase) name.toUpperCase() else name) + number\n\nfun useFoo() = listOf(\n        foo(\"a\"),\n        foo(\"b\", number = 1),\n        foo(\"c\", toUpperCase = true),\n        foo(name = \"d\", number = 2, toUpperCase = true)\n)",
      "import org.junit.Test\nimport org.junit.Assert\n\nclass TestDefaultAndNamedParams() {\n\n    @Test fun testDefaultAndNamedParams() {\n        Assert.assertEquals(listOf(\"a42\", \"b1\", \"C42\", \"D2\"), useFoo())\n    }\n}"
    )
  }

  @ParameterizedTest
  @EnumSource(ExecutorMode::class)
  fun `koans test lambdas`(mode: ExecutorMode) {
    runKoanTest(
      mode,
      "fun containsEven(collection: Collection<Int>): Boolean = collection.any { it % 2 == 0 }",
      "import org.junit.Test\nimport org.junit.Assert\n\nclass TestLambdas() {\n    @Test fun contains() {\n        Assert.assertTrue(\"The result should be true if the collection contains an even number\", containsEven(listOf(1, 2, 3, 126, 555)))\n    }\n\n    @Test fun notContains() {\n        Assert.assertFalse(\"The result should be false if the collection doesn't contain an even number\", containsEven(listOf(43, 33)))\n    }\n}"
    )
  }

  @ParameterizedTest
  @EnumSource(ExecutorMode::class)
  fun `koans test Strings`(mode: ExecutorMode) {
    runKoanTest(
      mode,
      "val month = \"(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)\"\n\nfun getPattern(): String = \"\"\"\\d{2} \${month} \\d{4}\"\"\"",
      "import org.junit.Test\nimport org.junit.Assert\nimport java.util.regex.Pattern\n\nclass TestStringTemplates() {\n    private fun testMatch(date: String) = Assert.assertTrue(\"The pattern should match \$date\", date.matches(getPattern().toRegex()))\n    private fun testMismatch(date: String) = Assert.assertFalse(\"The pattern shouldn't match \$date\", date.matches(getPattern().toRegex()))\n\n    @Test fun match() {\n        testMatch(\"11 MAR 1952\")\n    }\n\n    @Test fun match1() {\n        testMatch(\"24 AUG 1957\")\n    }\n\n    @Test fun doNotMatch() {\n        testMismatch(\"24 RRR 1957\")\n    }}"
    )

  }

  @ParameterizedTest
  @EnumSource(ExecutorMode::class)
  fun `koans test Data classes`(mode: ExecutorMode) {
    runKoanTest(
      mode,
      "data class Person(val name: String, val age: Int)\n\nfun getPeople(): List<Person> {\n    return listOf(Person(\"Alice\", 29), Person(\"Bob\", 31))\n}",
      "import org.junit.Test\nimport org.junit.Assert\n\n\nclass TestDataClasses {\n    @Test fun testListOfPeople() {\n        Assert.assertEquals(\"[Person(name=Alice, age=29), Person(name=Bob, age=31)]\", getPeople().toString())\n    }\n}"
    )
  }

  @ParameterizedTest
  @EnumSource(ExecutorMode::class)
  fun `koans test Nullable types`(mode: ExecutorMode) {
    runKoanTest(
      mode,
      "fun sendMessageToClient(\n        client: Client?, message: String?, mailer: Mailer\n){\n    val email = client?.personalInfo?.email\n    if (email != null && message != null) {\n        mailer.sendMessage(email, message)\n    }\n}\n\nclass Client (val personalInfo: PersonalInfo?)\nclass PersonalInfo (val email: String?)\ninterface Mailer {\n    fun sendMessage(email: String, message: String)\n}",
      "import org.junit.Test\nimport org.junit.Assert\n\nclass TestNullableTypes {\n    fun testSendMessageToClient(\n            client: Client?,\n            message: String?,\n            expectedEmail: String? = null,\n            shouldBeInvoked: Boolean = false\n    ) {\n        var invoked = false\n        val expectedMessage = message\n        sendMessageToClient(client, message, object : Mailer {\n            override fun sendMessage(email: String, message: String) {\n                invoked = true\n                Assert.assertEquals(\"The message is not as expected:\",\n                        expectedMessage, message)\n                Assert.assertEquals(\"The email is not as expected:\",\n                        expectedEmail, email)\n            }\n        })\n        Assert.assertEquals(\"The function 'sendMessage' should\${if (shouldBeInvoked) \"\" else \"n't\"} be invoked\",\n                shouldBeInvoked, invoked)\n    }\n\n    @Test fun everythingIsOk() {\n        testSendMessageToClient(Client(PersonalInfo(\"bob@gmail.com\")),\n                \"Hi Bob! We have an awesome proposition for you...\",\n                \"bob@gmail.com\",\n                true)\n    }\n\n    @Test fun noMessage() {\n        testSendMessageToClient(Client(PersonalInfo(\"bob@gmail.com\")), null)\n    }\n\n    @Test fun noEmail() {\n        testSendMessageToClient(Client(PersonalInfo(null)), \"Hi Bob! We have an awesome proposition for you...\")\n    }\n\n    @Test fun noPersonalInfo() {\n        testSendMessageToClient(Client(null), \"Hi Bob! We have an awesome proposition for you...\")\n    }\n\n    @Test fun noClient() {\n        testSendMessageToClient(null, \"Hi Bob! We have an awesome proposition for you...\")\n    }\n}"
    )

  }


  @ParameterizedTest
  @EnumSource(ExecutorMode::class)
  fun `koans test Smart casts`(mode: ExecutorMode) {
    runKoanTest(
      mode,
      "fun eval(expr: Expr): Int =\n        when (expr) {\n            is Num -> expr.value\n            is Sum -> eval(expr.left) + eval(expr.right)\n            else -> throw IllegalArgumentException(\"Unknown expression\")\n        }\n\ninterface Expr\nclass Num(val value: Int) : Expr\nclass Sum(val left: Expr, val right: Expr) : Expr",
      "import org.junit.Test\nimport org.junit.Assert\n\nclass TestSmartCasts {\n    @Test fun testNum() {\n        Assert.assertEquals(\"'eval' on Num should work:\", 2, eval(Num(2)))\n    }\n\n    @Test fun testSum() {\n        Assert.assertEquals(\"'eval' on Sum should work:\", 3, eval(Sum(Num(2), Num(1))))\n    }\n\n    @Test fun testRecursion() {\n        Assert.assertEquals(\"'eval' should work:\", 6, eval(Sum(Sum(Num(1), Num(2)), Num(3))))\n    }\n}"
    )

  }

  @ParameterizedTest
  @EnumSource(ExecutorMode::class)
  fun `koans test Extension functions`(mode: ExecutorMode) {
    runKoanTest(
      mode,
      "fun Int.r(): RationalNumber = RationalNumber(this, 1)\nfun Pair<Int, Int>.r(): RationalNumber = RationalNumber(first, second)\n\ndata class RationalNumber(val numerator: Int, val denominator: Int)",
      "import org.junit.Test\nimport org.junit.Assert\n\nclass TestExtensionFunctions() {\n    @Test fun testIntExtension() {\n        Assert.assertEquals(\"Rational number creation error: \", RationalNumber(4, 1), 4.r())\n    }\n\n    @Test fun testPairExtension() {\n        Assert.assertEquals(\"Rational number creation error: \", RationalNumber(2, 3), Pair(2, 3).r())\n    }\n}"
    )
  }

  @ParameterizedTest
  @EnumSource(ExecutorMode::class)
  fun `koans test Object expressions`(mode: ExecutorMode) {
    runKoanTest(
      mode,
      "import java.util.*\n\nfun getList(): List<Int> {\n    val arrayList = arrayListOf(1, 5, 2)\n    Collections.sort(arrayList, object : Comparator<Int> {\n    override fun compare(x: Int, y: Int) = y - x\n})\n    return arrayList\n}",
      "import org.junit.Test\nimport org.junit.Assert\nimport koans.util.toMessageInEquals\n\nclass TestObjectExpressions {\n    @Test fun testSort() {\n        Assert.assertEquals(\"getList\".toMessageInEquals(), listOf(5, 2, 1), getList())\n    }\n}"
    )
  }

  @ParameterizedTest
  @EnumSource(ExecutorMode::class)
  fun `koans test SAM conversions`(mode: ExecutorMode) {
    runKoanTest(
      mode,
      "import java.util.*\n\nfun getList(): List<Int> {\n    val arrayList = arrayListOf(1, 5, 2)\n    Collections.sort(arrayList, { x, y -> y - x })\n    return arrayList\n}",
      "import org.junit.Test\nimport org.junit.Assert\nimport koans.util.toMessageInEquals\n\nclass TestSamConversions {\n    @Test fun testSort() {\n        Assert.assertEquals(\"getList\".toMessageInEquals(), listOf(5, 2, 1), getList())\n    }\n}"
    )

  }

  @ParameterizedTest
  @EnumSource(ExecutorMode::class)
  fun `koans test -Extensions on collections`(mode: ExecutorMode) {
    runKoanTest(
      mode,
      "fun getList(): List<Int> {\n    return arrayListOf(1, 5, 2).sortedDescending()\n}",
      "import org.junit.Test\nimport org.junit.Assert\n\nclass TestExtensionsOnCollections {\n    @Test fun testSort() {\n        Assert.assertEquals(listOf(5, 2, 1), getList())\n    }\n}"
    )
  }

  @ParameterizedTest
  @EnumSource(ExecutorMode::class)
  fun `koans test -Comparison`(mode: ExecutorMode) {
    runKoanTest(
      mode,
      "data class MyDate(val year: Int, val month: Int, val dayOfMonth: Int) : Comparable<MyDate> {\n    override fun compareTo(other: MyDate) = when {\n        year != other.year -> year - other.year\n        month != other.month -> month - other.month\n        else -> dayOfMonth - other.dayOfMonth\n    }\n}\n\nfun compare(date1: MyDate, date2: MyDate) = date1 < date2",
      "import org.junit.Assert\nimport org.junit.Test\nimport koans.util.toMessage\n\nclass TestComparison {\n\n    @Test fun testBefore() {\n        val first = MyDate(2014, 5, 10)\n        val second = MyDate(2014, 7, 11)\n        Assert.assertTrue(\"compareTo\".toMessage() + \": \${first} should go before \${second}\", first < second)\n    }\n\n    @Test fun testAfter() {\n        val first = MyDate(2014, 10, 20)\n        val second = MyDate(2014, 7, 11)\n        Assert.assertTrue(\"compareTo\".toMessage() + \": \${first} should go after \${second}\", first > second)\n    }\n}"
    )

  }

  @ParameterizedTest
  @EnumSource(ExecutorMode::class)
  fun `koans test in range`(mode: ExecutorMode) {
    runKoanTest(
      mode,
      "class DateRange(val start: MyDate, val endInclusive: MyDate){\n    operator fun contains(item: MyDate): Boolean = start <= item && item <= endInclusive\n}\n\nfun checkInRange(date: MyDate, first: MyDate, last: MyDate): Boolean {\n    return date in DateRange(first, last)\n}",
      "import org.junit.Assert\nimport org.junit.Test\nimport koans.util.inEquals\n\nclass TestInRange {\n    fun doTest(date: MyDate, first: MyDate, last: MyDate, shouldBeInRange: Boolean) {\n        val message = \"\${date} should\${if (shouldBeInRange) \"\" else \"n't\"} be in \${DateRange(first, last)}\".inEquals()\n        Assert.assertEquals(message, shouldBeInRange, checkInRange(date, first, last))\n    }\n\n    @Test fun testInRange() {\n        doTest(MyDate(2014, 3, 22), MyDate(2014, 1, 1), MyDate(2015, 1, 1), shouldBeInRange = true)\n    }\n\n    @Test fun testBefore() {\n        doTest(MyDate(2013, 3, 22), MyDate(2014, 1, 1), MyDate(2015, 1, 1), shouldBeInRange = false)\n    }\n\n    @Test fun testAfter() {\n        doTest(MyDate(2015, 3, 22), MyDate(2014, 1, 1), MyDate(2015, 1, 1), shouldBeInRange = false)\n    }\n}",
      "data class MyDate(val year: Int, val month: Int, val dayOfMonth: Int) : Comparable<MyDate> {\n    override fun compareTo(other: MyDate) = when {\n        year != other.year -> year - other.year\n        month != other.month -> month - other.month\n        else -> dayOfMonth - other.dayOfMonth\n    }\n}"
    )

  }

  @ParameterizedTest
  @EnumSource(ExecutorMode::class)
  fun `koans test Range to`(mode: ExecutorMode) {
    runKoanTest(
      mode,
      "operator fun MyDate.rangeTo(other: MyDate) = DateRange(this, other)\n\nclass DateRange(override val start: MyDate, override val endInclusive: MyDate): ClosedRange<MyDate>\n\nfun checkInRange(date: MyDate, first: MyDate, last: MyDate): Boolean {\n    return date in first..last\n}",
      "import org.junit.Assert\nimport org.junit.Test\nimport java.util.ArrayList\nimport koans.util.inEquals\n\nclass TestRangeTo {\n    fun doTest(date: MyDate, first: MyDate, last: MyDate, shouldBeInRange: Boolean) {\n        val message = \"\${date} should\${if (shouldBeInRange) \"\" else \"n't\"} be in range: \${first}..\${last}\".inEquals()\n        Assert.assertEquals(message, shouldBeInRange, checkInRange(date, first, last))\n    }\n\n    @Test fun testInRange() {\n        doTest(MyDate(2014, 3, 22), MyDate(2014, 1, 1), MyDate(2015, 1, 1), shouldBeInRange = true)\n    }\n}",
      "data class MyDate(val year: Int, val month: Int, val dayOfMonth: Int) : Comparable<MyDate> {\n    override fun compareTo(other: MyDate) = when {\n        year != other.year -> year - other.year\n        month != other.month -> month - other.month\n        else -> dayOfMonth - other.dayOfMonth\n    }\n}"
    )

  }

  @ParameterizedTest
  @EnumSource(ExecutorMode::class)
  fun `koans test For loop`(mode: ExecutorMode) {
    runKoanTest(
      mode,
      "class DateRange(val start: MyDate, val end: MyDate): Iterable<MyDate>{\n    override fun iterator(): Iterator<MyDate> = DateIterator(this)\n}\n\nclass DateIterator(val dateRange:DateRange) : Iterator<MyDate> {\n    var current: MyDate = dateRange.start\n    override fun next(): MyDate {\n        val result = current\n        current = current.nextDay()\n        return result\n    }\n    override fun hasNext(): Boolean = current <= dateRange.end\n}\n\nfun iterateOverDateRange(firstDate: MyDate, secondDate: MyDate, handler: (MyDate) -> Unit) {\n    for (date in firstDate..secondDate) {\n        handler(date)\n    }\n}",
      "import org.junit.Assert\nimport org.junit.Test\nimport koans.util.inEquals\n\nclass TestForLoop {\n    @Test fun testIterateOverDateRange() {\n        val actualDateRange = arrayListOf<MyDate>()\n        iterateOverDateRange(MyDate(2016, 5, 1), MyDate(2016, 5, 5), {\n            date-> actualDateRange.add(date)\n        })\n        val expectedDateRange = arrayListOf(\n                MyDate(2016, 5, 1), MyDate(2016, 5, 2), MyDate(2016, 5, 3), MyDate(2016, 5, 4), MyDate(2016, 5, 5))\n        Assert.assertEquals(\"Incorrect iteration over five nice spring dates\".inEquals(),\n                expectedDateRange, actualDateRange)\n    }\n\n    @Test fun testIterateOverEmptyRange() {\n        var invoked = false\n        iterateOverDateRange(MyDate(2016, 1, 1), MyDate(2015, 1, 1), { invoked = true })\n        Assert.assertFalse(\"Handler was invoked on an empty range\".inEquals(), invoked)\n    }\n}",
      "data class MyDate(val year: Int, val month: Int, val dayOfMonth: Int) : Comparable<MyDate> {\n    override fun compareTo(other: MyDate) = when {\n        year != other.year -> year - other.year\n        month != other.month -> month - other.month\n        else -> dayOfMonth - other.dayOfMonth\n    }\n}\n\noperator fun MyDate.rangeTo(other: MyDate) = DateRange(this, other)",
      "import java.util.Calendar\n\nfun MyDate.nextDay() = addTimeIntervals(TimeInterval.DAY, 1)\n\nenum class TimeInterval {\n    DAY,\n    WEEK,\n    YEAR\n}\n\nfun MyDate.addTimeIntervals(timeInterval: TimeInterval, number: Int): MyDate {\n    val c = Calendar.getInstance()\n    c.set(year, month, dayOfMonth)\n    when (timeInterval) {\n        TimeInterval.DAY -> c.add(Calendar.DAY_OF_MONTH, number)\n        TimeInterval.WEEK -> c.add(Calendar.WEEK_OF_MONTH, number)\n        TimeInterval.YEAR -> c.add(Calendar.YEAR, number)\n    }\n    return MyDate(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DATE))\n}"
    )

  }

  @ParameterizedTest
  @EnumSource(ExecutorMode::class)
  fun `koans test Operators overloading`(mode: ExecutorMode) {
    runKoanTest(
      mode,
      "import TimeInterval.*\n\ndata class MyDate(val year: Int, val month: Int, val dayOfMonth: Int)\n\nenum class TimeInterval { DAY, WEEK, YEAR }\n\noperator fun MyDate.plus(timeInterval: TimeInterval) = addTimeIntervals(timeInterval, 1)\n\nclass RepeatedTimeInterval(val timeInterval: TimeInterval, val number: Int)\noperator fun TimeInterval.times(number: Int) = RepeatedTimeInterval(this, number)\n\noperator fun MyDate.plus(timeIntervals: RepeatedTimeInterval) = addTimeIntervals(timeIntervals.timeInterval, timeIntervals.number)\n\nfun task1(today: MyDate): MyDate {\n    return today + YEAR + WEEK\n}\n\nfun task2(today: MyDate): MyDate {\n    return today + YEAR * 2 + WEEK * 3 + DAY * 5\n}",
      "import org.junit.Assert\nimport org.junit.Test\nimport koans.util.toMessageInEquals\n\nclass TestOperatorsOverloading {\n    @Test fun testAddOneTimeInterval() {\n        Assert.assertEquals(\"task1\".toMessageInEquals(), MyDate(2015, 5, 8), task1(MyDate(2014, 5, 1)))\n    }\n\n    @Test fun testOneMonth() {\n        Assert.assertEquals(\"task2\".toMessageInEquals(), MyDate(2016, 0, 27), task2(MyDate(2014, 0, 1)))\n    }\n\n    @Test fun testMonthChange() {\n        Assert.assertEquals(\"task2\".toMessageInEquals(), MyDate(2016, 1, 20), task2(MyDate(2014, 0, 25)))\n    }\n}",
      "import java.util.Calendar\n\nfun MyDate.addTimeIntervals(timeInterval: TimeInterval, number: Int): MyDate {\n    val c = Calendar.getInstance()\n    c.set(year, month, dayOfMonth)\n    when (timeInterval) {\n        TimeInterval.DAY -> c.add(Calendar.DAY_OF_MONTH, number)\n        TimeInterval.WEEK -> c.add(Calendar.WEEK_OF_MONTH, number)\n        TimeInterval.YEAR -> c.add(Calendar.YEAR, number)\n    }\n    return MyDate(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DATE))\n}"
    )
  }

  @ParameterizedTest
  @EnumSource(ExecutorMode::class)
  fun `koans test Destructuring declarations`(mode: ExecutorMode) {
    runKoanTest(
      mode,
      "data class MyDate(val year: Int, val month: Int, val dayOfMonth: Int)\n\nfun isLeapDay(date: MyDate): Boolean {\n\n    val (year, month, dayOfMonth) = date\n\n    // 29 February of a leap year\n    return year % 4 == 0 && month == 2 && dayOfMonth == 29\n}",
      "import org.junit.Assert\nimport org.junit.Test\n\nclass TestMultiAssignment {\n    @Test fun testIsLeapDay() {\n        Assert.assertTrue(\"The test failed\", isLeapDay(MyDate(2016, 2, 29)))\n        Assert.assertFalse(\"The test failed\", isLeapDay(MyDate(2015, 2, 29)))\n    }\n}"
    )
  }

  @ParameterizedTest
  @EnumSource(ExecutorMode::class)
  fun `koans test Invoke`(mode: ExecutorMode) {
    runKoanTest(
      mode,
      "class Invokable {\n    var numberOfInvocations: Int = 0\n        private set\n    operator fun invoke(): Invokable {\n        numberOfInvocations++\n        return this\n    }\n}\n\nfun invokeTwice(invokable: Invokable) = invokable()()",
      "import org.junit.Assert\nimport org.junit.Test\nimport koans.util.inEquals\n\nclass TestInvoke {\n    @Test fun testInvokeTwice() = testInvokable(2, ::invokeTwice)\n\n    private fun testInvokable(numberOfInvocations: Int, invokeSeveralTimes: (Invokable) -> Invokable) {\n        val invokable = Invokable()\n        val message = \"The number of invocations is incorrect\".inEquals()\n        Assert.assertEquals(message, numberOfInvocations, invokeSeveralTimes(invokable).numberOfInvocations)\n    }\n\n    @Test fun testNumberOfInvocations() {\n        testInvokable(1) { it() }\n        testInvokable(5) { it()()()()() }\n        testInvokable(0) { it }\n    }\n}"
    )
  }

  @ParameterizedTest
  @EnumSource(ExecutorMode::class)
  fun `koans test Introduction`(mode: ExecutorMode) {
    runKoanTest(
      mode,
      "fun Shop.getSetOfCustomers(): Set<Customer> = customers.toSet()",
      "import org.junit.Assert\nimport org.junit.Test\nimport koans.util.toMessage\n\nclass TestIntroduction {\n    @Test fun testSetOfCustomers(){\n        Assert.assertEquals(\"getSetOfCustomers\".toMessage(),\n                customers.values.toSet(), shop.getSetOfCustomers())\n    }\n}",
      "//products\nval idea = Product(\"IntelliJ IDEA Ultimate\", 199.0)\nval reSharper = Product(\"ReSharper\", 149.0)\nval dotTrace = Product(\"DotTrace\", 159.0)\nval dotMemory = Product(\"DotTrace\", 129.0)\nval dotCover = Product(\"DotCover\", 99.0)\nval appCode = Product(\"AppCode\", 99.0)\nval phpStorm = Product(\"PhpStorm\", 99.0)\nval pyCharm = Product(\"PyCharm\", 99.0)\nval rubyMine = Product(\"RubyMine\", 99.0)\nval webStorm = Product(\"WebStorm\", 49.0)\nval teamCity = Product(\"TeamCity\", 299.0)\nval youTrack = Product(\"YouTrack\", 500.0)\n\n//customers\nval lucas = \"Lucas\"\nval cooper = \"Cooper\"\nval nathan = \"Nathan\"\nval reka = \"Reka\"\nval bajram = \"Bajram\"\nval asuka = \"Asuka\"\nval riku = \"Riku\"\n\n//cities\nval Canberra = City(\"Canberra\")\nval Vancouver = City(\"Vancouver\")\nval Budapest = City(\"Budapest\")\nval Ankara = City(\"Ankara\")\nval Tokyo = City(\"Tokyo\")\n\nfun customer(name: String, city: City, vararg orders: Order) = Customer(name, city, orders.toList())\nfun order(vararg products: Product, isDelivered: Boolean = true) = Order(products.toList(), isDelivered)\nfun shop(name: String, vararg customers: Customer) = Shop(name, customers.toList())\n\nval shop = shop(\"jb test shop\",\n        customer(lucas, Canberra,\n                order(reSharper),\n                order(reSharper, dotMemory, dotTrace)\n        ),\n        customer(cooper, Canberra),\n        customer(nathan, Vancouver,\n                order(rubyMine, webStorm)\n        ),\n        customer(reka, Budapest,\n                order(idea, isDelivered = false),\n                order(idea, isDelivered = false),\n                order(idea)\n        ),\n        customer(bajram, Ankara,\n                order(reSharper)\n        ),\n        customer(asuka, Tokyo,\n                order(idea)\n        ),\n        customer(riku, Tokyo,\n                order(phpStorm, phpStorm),\n                order(phpStorm)\n        )\n\n)\n\nval customers: Map<String, Customer> = shop.customers.fold(hashMapOf<String, Customer>(), {\n        map, customer ->\n        map[customer.name] = customer\n        map\n})\n\nval orderedProducts = setOf(idea, reSharper, dotTrace, dotMemory, rubyMine, webStorm, phpStorm)\n\nval sortedCustomers = listOf(cooper, nathan, bajram, asuka, lucas, riku, reka).map { customers[it] }\n\nval groupedByCities = mapOf(\n        Canberra to listOf(lucas, cooper),\n        Vancouver to listOf(nathan),\n        Budapest to listOf(reka),\n        Ankara to listOf(bajram),\n        Tokyo to listOf(asuka, riku)\n).mapValues { it.value.map { name -> customers[name] } }",
      "data class Shop(val name: String, val customers: List<Customer>)\n\ndata class Customer(val name: String, val city: City, val orders: List<Order>) {\n    override fun toString() = \"\$name from \${city.name}\"\n}\n\ndata class Order(val products: List<Product>, val isDelivered: Boolean)\n\ndata class Product(val name: String, val price: Double) {\n    override fun toString() = \"'\$name' for \$price\"\n}\n\ndata class City(val name: String) {\n    override fun toString() = name\n}"
    )
  }

  @ParameterizedTest
  @EnumSource(ExecutorMode::class)
  fun `koans test Filter map`(mode: ExecutorMode) {
    runKoanTest(
      mode,
      "// Return the set of cities the customers are from\nfun Shop.getCitiesCustomersAreFrom(): Set<City> = customers.map { it.city }.toSet()\n\n// Return a list of the customers who live in the given city\nfun Shop.getCustomersFrom(city: City): List<Customer> = customers.filter { it.city == city }",
      "import org.junit.Assert\nimport org.junit.Test\nimport koans.util.toMessage\n\nclass TestFilterMap {\n    @Test fun testCitiesCustomersAreFrom() {\n        Assert.assertEquals(\"getCitiesCustomersAreFrom\".toMessage(),\n                setOf(Canberra, Vancouver, Budapest, Ankara, Tokyo), shop.getCitiesCustomersAreFrom())\n    }\n\n    @Test fun testCustomersFromCity() {\n        Assert.assertEquals(\"getCustomersFrom\".toMessage(),\n                listOf(customers[lucas], customers[cooper]), shop.getCustomersFrom(Canberra))\n    }\n}",
      "//products\nval idea = Product(\"IntelliJ IDEA Ultimate\", 199.0)\nval reSharper = Product(\"ReSharper\", 149.0)\nval dotTrace = Product(\"DotTrace\", 159.0)\nval dotMemory = Product(\"DotTrace\", 129.0)\nval dotCover = Product(\"DotCover\", 99.0)\nval appCode = Product(\"AppCode\", 99.0)\nval phpStorm = Product(\"PhpStorm\", 99.0)\nval pyCharm = Product(\"PyCharm\", 99.0)\nval rubyMine = Product(\"RubyMine\", 99.0)\nval webStorm = Product(\"WebStorm\", 49.0)\nval teamCity = Product(\"TeamCity\", 299.0)\nval youTrack = Product(\"YouTrack\", 500.0)\n\n//customers\nval lucas = \"Lucas\"\nval cooper = \"Cooper\"\nval nathan = \"Nathan\"\nval reka = \"Reka\"\nval bajram = \"Bajram\"\nval asuka = \"Asuka\"\nval riku = \"Riku\"\n\n//cities\nval Canberra = City(\"Canberra\")\nval Vancouver = City(\"Vancouver\")\nval Budapest = City(\"Budapest\")\nval Ankara = City(\"Ankara\")\nval Tokyo = City(\"Tokyo\")\n\nfun customer(name: String, city: City, vararg orders: Order) = Customer(name, city, orders.toList())\nfun order(vararg products: Product, isDelivered: Boolean = true) = Order(products.toList(), isDelivered)\nfun shop(name: String, vararg customers: Customer) = Shop(name, customers.toList())\n\nval shop = shop(\"jb test shop\",\n        customer(lucas, Canberra,\n                order(reSharper),\n                order(reSharper, dotMemory, dotTrace)\n        ),\n        customer(cooper, Canberra),\n        customer(nathan, Vancouver,\n                order(rubyMine, webStorm)\n        ),\n        customer(reka, Budapest,\n                order(idea, isDelivered = false),\n                order(idea, isDelivered = false),\n                order(idea)\n        ),\n        customer(bajram, Ankara,\n                order(reSharper)\n        ),\n        customer(asuka, Tokyo,\n                order(idea)\n        ),\n        customer(riku, Tokyo,\n                order(phpStorm, phpStorm),\n                order(phpStorm)\n        )\n\n)\n\nval customers: Map<String, Customer> = shop.customers.fold(hashMapOf<String, Customer>(), {\n        map, customer ->\n        map[customer.name] = customer\n        map\n})\n\nval orderedProducts = setOf(idea, reSharper, dotTrace, dotMemory, rubyMine, webStorm, phpStorm)\n\nval sortedCustomers = listOf(cooper, nathan, bajram, asuka, lucas, riku, reka).map { customers[it] }\n\nval groupedByCities = mapOf(\n        Canberra to listOf(lucas, cooper),\n        Vancouver to listOf(nathan),\n        Budapest to listOf(reka),\n        Ankara to listOf(bajram),\n        Tokyo to listOf(asuka, riku)\n).mapValues { it.value.map { name -> customers[name] } }",
      "data class Shop(val name: String, val customers: List<Customer>)\n\ndata class Customer(val name: String, val city: City, val orders: List<Order>) {\n    override fun toString() = \"\$name from \${city.name}\"\n}\n\ndata class Order(val products: List<Product>, val isDelivered: Boolean)\n\ndata class Product(val name: String, val price: Double) {\n    override fun toString() = \"'\$name' for \$price\"\n}\n\ndata class City(val name: String) {\n    override fun toString() = name\n}"
    )
  }

  @ParameterizedTest
  @EnumSource(ExecutorMode::class)
  fun `koans test All Any and other predicates`(mode: ExecutorMode) {
    runKoanTest(
      mode,
      "// Return true if all customers are from the given city\nfun Shop.checkAllCustomersAreFrom(city: City): Boolean = customers.all { it.city == city }\n\n// Return true if there is at least one customer from the given city\nfun Shop.hasCustomerFrom(city: City): Boolean = customers.any { it.city == city }\n\n// Return the number of customers from the given city\nfun Shop.countCustomersFrom(city: City): Int = customers.count { it.city == city }\n\n// Return a customer who lives in the given city, or null if there is none\nfun Shop.findAnyCustomerFrom(city: City): Customer? = customers.find { it.city == city }",
      "import org.junit.Assert\nimport org.junit.Test\nimport koans.util.toMessage\n\nclass TestAllAnyAndOtherPredicates {\n\n    @Test fun testAllCustomersAreFromCity() {\n        Assert.assertFalse(\"checkAllCustomersAreFrom\".toMessage(),\n                shop.checkAllCustomersAreFrom(Canberra))\n    }\n\n    @Test fun testAnyCustomerIsFromCity() {\n        Assert.assertTrue(\"hasCustomerFrom\".toMessage(), shop.hasCustomerFrom(Canberra))\n    }\n\n    @Test fun testCountCustomersFromCity() {\n        Assert.assertEquals(\"countCustomersFrom\".toMessage(), 2, shop.countCustomersFrom(Canberra))\n    }\n\n    @Test fun testAnyCustomerFromCity() {\n        Assert.assertEquals(\"findAnyCustomerFrom\".toMessage(), customers[lucas], shop.findAnyCustomerFrom(Canberra))\n        Assert.assertEquals(\"findAnyCustomerFrom\".toMessage(), null, shop.findAnyCustomerFrom(City(\"Chicago\")))\n    }\n}",
      "//products\nval idea = Product(\"IntelliJ IDEA Ultimate\", 199.0)\nval reSharper = Product(\"ReSharper\", 149.0)\nval dotTrace = Product(\"DotTrace\", 159.0)\nval dotMemory = Product(\"DotTrace\", 129.0)\nval dotCover = Product(\"DotCover\", 99.0)\nval appCode = Product(\"AppCode\", 99.0)\nval phpStorm = Product(\"PhpStorm\", 99.0)\nval pyCharm = Product(\"PyCharm\", 99.0)\nval rubyMine = Product(\"RubyMine\", 99.0)\nval webStorm = Product(\"WebStorm\", 49.0)\nval teamCity = Product(\"TeamCity\", 299.0)\nval youTrack = Product(\"YouTrack\", 500.0)\n\n//customers\nval lucas = \"Lucas\"\nval cooper = \"Cooper\"\nval nathan = \"Nathan\"\nval reka = \"Reka\"\nval bajram = \"Bajram\"\nval asuka = \"Asuka\"\nval riku = \"Riku\"\n\n//cities\nval Canberra = City(\"Canberra\")\nval Vancouver = City(\"Vancouver\")\nval Budapest = City(\"Budapest\")\nval Ankara = City(\"Ankara\")\nval Tokyo = City(\"Tokyo\")\n\nfun customer(name: String, city: City, vararg orders: Order) = Customer(name, city, orders.toList())\nfun order(vararg products: Product, isDelivered: Boolean = true) = Order(products.toList(), isDelivered)\nfun shop(name: String, vararg customers: Customer) = Shop(name, customers.toList())\n\nval shop = shop(\"jb test shop\",\n        customer(lucas, Canberra,\n                order(reSharper),\n                order(reSharper, dotMemory, dotTrace)\n        ),\n        customer(cooper, Canberra),\n        customer(nathan, Vancouver,\n                order(rubyMine, webStorm)\n        ),\n        customer(reka, Budapest,\n                order(idea, isDelivered = false),\n                order(idea, isDelivered = false),\n                order(idea)\n        ),\n        customer(bajram, Ankara,\n                order(reSharper)\n        ),\n        customer(asuka, Tokyo,\n                order(idea)\n        ),\n        customer(riku, Tokyo,\n                order(phpStorm, phpStorm),\n                order(phpStorm)\n        )\n\n)\n\nval customers: Map<String, Customer> = shop.customers.fold(hashMapOf<String, Customer>(), {\n        map, customer ->\n        map[customer.name] = customer\n        map\n})\n\nval orderedProducts = setOf(idea, reSharper, dotTrace, dotMemory, rubyMine, webStorm, phpStorm)\n\nval sortedCustomers = listOf(cooper, nathan, bajram, asuka, lucas, riku, reka).map { customers[it] }\n\nval groupedByCities = mapOf(\n        Canberra to listOf(lucas, cooper),\n        Vancouver to listOf(nathan),\n        Budapest to listOf(reka),\n        Ankara to listOf(bajram),\n        Tokyo to listOf(asuka, riku)\n).mapValues { it.value.map { name -> customers[name] } }",
      "data class Shop(val name: String, val customers: List<Customer>)\n\ndata class Customer(val name: String, val city: City, val orders: List<Order>) {\n    override fun toString() = \"\$name from \${city.name}\"\n}\n\ndata class Order(val products: List<Product>, val isDelivered: Boolean)\n\ndata class Product(val name: String, val price: Double) {\n    override fun toString() = \"'\$name' for \$price\"\n}\n\ndata class City(val name: String) {\n    override fun toString() = name\n}"
    )
  }

  @ParameterizedTest
  @EnumSource(ExecutorMode::class)
  fun `koans test FlatMap`(mode: ExecutorMode) {
    runKoanTest(
      mode,
      "// Return all products this customer has ordered\nval Customer.orderedProducts: Set<Product> get() {\n    return orders.flatMap { it.products }.toSet()\n}\n\n// Return all products that were ordered by at least one customer\nval Shop.allOrderedProducts: Set<Product> get() {\n    return customers.flatMap { it.orderedProducts }.toSet()\n}",
      "import org.junit.Assert\nimport org.junit.Test\nimport koans.util.toMessage\n\nclass TestFlatMap {\n    @Test fun testGetOrderedProductsSet() {\n        Assert.assertEquals(\"getOrderedProducts\".toMessage(),\n                setOf(idea), customers[reka]!!.orderedProducts)\n    }\n\n    @Test fun testGetAllOrderedProducts() {\n        Assert.assertEquals(\"getAllOrderedProducts\".toMessage(),\n                orderedProducts, shop.allOrderedProducts)\n    }\n}",
      "//products\nval idea = Product(\"IntelliJ IDEA Ultimate\", 199.0)\nval reSharper = Product(\"ReSharper\", 149.0)\nval dotTrace = Product(\"DotTrace\", 159.0)\nval dotMemory = Product(\"DotTrace\", 129.0)\nval dotCover = Product(\"DotCover\", 99.0)\nval appCode = Product(\"AppCode\", 99.0)\nval phpStorm = Product(\"PhpStorm\", 99.0)\nval pyCharm = Product(\"PyCharm\", 99.0)\nval rubyMine = Product(\"RubyMine\", 99.0)\nval webStorm = Product(\"WebStorm\", 49.0)\nval teamCity = Product(\"TeamCity\", 299.0)\nval youTrack = Product(\"YouTrack\", 500.0)\n\n//customers\nval lucas = \"Lucas\"\nval cooper = \"Cooper\"\nval nathan = \"Nathan\"\nval reka = \"Reka\"\nval bajram = \"Bajram\"\nval asuka = \"Asuka\"\nval riku = \"Riku\"\n\n//cities\nval Canberra = City(\"Canberra\")\nval Vancouver = City(\"Vancouver\")\nval Budapest = City(\"Budapest\")\nval Ankara = City(\"Ankara\")\nval Tokyo = City(\"Tokyo\")\n\nfun customer(name: String, city: City, vararg orders: Order) = Customer(name, city, orders.toList())\nfun order(vararg products: Product, isDelivered: Boolean = true) = Order(products.toList(), isDelivered)\nfun shop(name: String, vararg customers: Customer) = Shop(name, customers.toList())\n\nval shop = shop(\"jb test shop\",\n        customer(lucas, Canberra,\n                order(reSharper),\n                order(reSharper, dotMemory, dotTrace)\n        ),\n        customer(cooper, Canberra),\n        customer(nathan, Vancouver,\n                order(rubyMine, webStorm)\n        ),\n        customer(reka, Budapest,\n                order(idea, isDelivered = false),\n                order(idea, isDelivered = false),\n                order(idea)\n        ),\n        customer(bajram, Ankara,\n                order(reSharper)\n        ),\n        customer(asuka, Tokyo,\n                order(idea)\n        ),\n        customer(riku, Tokyo,\n                order(phpStorm, phpStorm),\n                order(phpStorm)\n        )\n\n)\n\nval customers: Map<String, Customer> = shop.customers.fold(hashMapOf<String, Customer>(), {\n        map, customer ->\n        map[customer.name] = customer\n        map\n})\n\nval orderedProducts = setOf(idea, reSharper, dotTrace, dotMemory, rubyMine, webStorm, phpStorm)\n\nval sortedCustomers = listOf(cooper, nathan, bajram, asuka, lucas, riku, reka).map { customers[it] }\n\nval groupedByCities = mapOf(\n        Canberra to listOf(lucas, cooper),\n        Vancouver to listOf(nathan),\n        Budapest to listOf(reka),\n        Ankara to listOf(bajram),\n        Tokyo to listOf(asuka, riku)\n).mapValues { it.value.map { name -> customers[name] } }",
      "data class Shop(val name: String, val customers: List<Customer>)\n\ndata class Customer(val name: String, val city: City, val orders: List<Order>) {\n    override fun toString() = \"\$name from \${city.name}\"\n}\n\ndata class Order(val products: List<Product>, val isDelivered: Boolean)\n\ndata class Product(val name: String, val price: Double) {\n    override fun toString() = \"'\$name' for \$price\"\n}\n\ndata class City(val name: String) {\n    override fun toString() = name\n}"
    )
  }

  @ParameterizedTest
  @EnumSource(ExecutorMode::class)
  fun `koans test Max min`(mode: ExecutorMode) {
    runKoanTest(
      mode,
      "// Return a customer whose order count is the highest among all customers\nfun Shop.getCustomerWithMaximumNumberOfOrders(): Customer? = customers.maxBy { it.orders.size }\n\n// Return the most expensive product which has been ordered\nfun Customer.getMostExpensiveOrderedProduct(): Product? = orders.flatMap { it.products }.maxBy { it.price }",
      "import org.junit.Assert\nimport org.junit.Test\nimport koans.util.toMessage\n\nclass TestMaxMin {\n    @Test fun testCustomerWithMaximumNumberOfOrders() {\n        Assert.assertEquals(\"getCustomerWithMaximumNumberOfOrders\".toMessage(),\n                customers[reka], shop.getCustomerWithMaximumNumberOfOrders())\n    }\n\n    @Test fun testTheMostExpensiveOrderedProduct() {\n        Assert.assertEquals(\"getMostExpensiveOrderedProduct\".toMessage(),\n                rubyMine, customers[nathan]!!.getMostExpensiveOrderedProduct())\n    }\n}",
      "//products\nval idea = Product(\"IntelliJ IDEA Ultimate\", 199.0)\nval reSharper = Product(\"ReSharper\", 149.0)\nval dotTrace = Product(\"DotTrace\", 159.0)\nval dotMemory = Product(\"DotTrace\", 129.0)\nval dotCover = Product(\"DotCover\", 99.0)\nval appCode = Product(\"AppCode\", 99.0)\nval phpStorm = Product(\"PhpStorm\", 99.0)\nval pyCharm = Product(\"PyCharm\", 99.0)\nval rubyMine = Product(\"RubyMine\", 99.0)\nval webStorm = Product(\"WebStorm\", 49.0)\nval teamCity = Product(\"TeamCity\", 299.0)\nval youTrack = Product(\"YouTrack\", 500.0)\n\n//customers\nval lucas = \"Lucas\"\nval cooper = \"Cooper\"\nval nathan = \"Nathan\"\nval reka = \"Reka\"\nval bajram = \"Bajram\"\nval asuka = \"Asuka\"\nval riku = \"Riku\"\n\n//cities\nval Canberra = City(\"Canberra\")\nval Vancouver = City(\"Vancouver\")\nval Budapest = City(\"Budapest\")\nval Ankara = City(\"Ankara\")\nval Tokyo = City(\"Tokyo\")\n\nfun customer(name: String, city: City, vararg orders: Order) = Customer(name, city, orders.toList())\nfun order(vararg products: Product, isDelivered: Boolean = true) = Order(products.toList(), isDelivered)\nfun shop(name: String, vararg customers: Customer) = Shop(name, customers.toList())\n\nval shop = shop(\"jb test shop\",\n        customer(lucas, Canberra,\n                order(reSharper),\n                order(reSharper, dotMemory, dotTrace)\n        ),\n        customer(cooper, Canberra),\n        customer(nathan, Vancouver,\n                order(rubyMine, webStorm)\n        ),\n        customer(reka, Budapest,\n                order(idea, isDelivered = false),\n                order(idea, isDelivered = false),\n                order(idea)\n        ),\n        customer(bajram, Ankara,\n                order(reSharper)\n        ),\n        customer(asuka, Tokyo,\n                order(idea)\n        ),\n        customer(riku, Tokyo,\n                order(phpStorm, phpStorm),\n                order(phpStorm)\n        )\n\n)\n\nval customers: Map<String, Customer> = shop.customers.fold(hashMapOf<String, Customer>(), {\n        map, customer ->\n        map[customer.name] = customer\n        map\n})\n\nval orderedProducts = setOf(idea, reSharper, dotTrace, dotMemory, rubyMine, webStorm, phpStorm)\n\nval sortedCustomers = listOf(cooper, nathan, bajram, asuka, lucas, riku, reka).map { customers[it] }\n\nval groupedByCities = mapOf(\n        Canberra to listOf(lucas, cooper),\n        Vancouver to listOf(nathan),\n        Budapest to listOf(reka),\n        Ankara to listOf(bajram),\n        Tokyo to listOf(asuka, riku)\n).mapValues { it.value.map { name -> customers[name] } }",
      "data class Shop(val name: String, val customers: List<Customer>)\n\ndata class Customer(val name: String, val city: City, val orders: List<Order>) {\n    override fun toString() = \"\$name from \${city.name}\"\n}\n\ndata class Order(val products: List<Product>, val isDelivered: Boolean)\n\ndata class Product(val name: String, val price: Double) {\n    override fun toString() = \"'\$name' for \$price\"\n}\n\ndata class City(val name: String) {\n    override fun toString() = name\n}"
    )
  }

  @ParameterizedTest
  @EnumSource(ExecutorMode::class)
  fun `koans test sort`(mode: ExecutorMode) {
    runKoanTest(
      mode,
      "// Return a list of customers, sorted by the ascending number of orders they made\nfun Shop.getCustomersSortedByNumberOfOrders(): List<Customer> = customers.sortedBy { it.orders.size }",
      "import org.junit.Assert\nimport org.junit.Test\nimport koans.util.toMessage\n\nclass TestSort {\n    @Test fun testGetCustomersSortedByNumberOfOrders() {\n        Assert.assertEquals(\"getCustomersSortedByNumberOfOrders\".toMessage(),\n                sortedCustomers, shop.getCustomersSortedByNumberOfOrders())\n\n    }\n}",
      "//products\nval idea = Product(\"IntelliJ IDEA Ultimate\", 199.0)\nval reSharper = Product(\"ReSharper\", 149.0)\nval dotTrace = Product(\"DotTrace\", 159.0)\nval dotMemory = Product(\"DotTrace\", 129.0)\nval dotCover = Product(\"DotCover\", 99.0)\nval appCode = Product(\"AppCode\", 99.0)\nval phpStorm = Product(\"PhpStorm\", 99.0)\nval pyCharm = Product(\"PyCharm\", 99.0)\nval rubyMine = Product(\"RubyMine\", 99.0)\nval webStorm = Product(\"WebStorm\", 49.0)\nval teamCity = Product(\"TeamCity\", 299.0)\nval youTrack = Product(\"YouTrack\", 500.0)\n\n//customers\nval lucas = \"Lucas\"\nval cooper = \"Cooper\"\nval nathan = \"Nathan\"\nval reka = \"Reka\"\nval bajram = \"Bajram\"\nval asuka = \"Asuka\"\nval riku = \"Riku\"\n\n//cities\nval Canberra = City(\"Canberra\")\nval Vancouver = City(\"Vancouver\")\nval Budapest = City(\"Budapest\")\nval Ankara = City(\"Ankara\")\nval Tokyo = City(\"Tokyo\")\n\nfun customer(name: String, city: City, vararg orders: Order) = Customer(name, city, orders.toList())\nfun order(vararg products: Product, isDelivered: Boolean = true) = Order(products.toList(), isDelivered)\nfun shop(name: String, vararg customers: Customer) = Shop(name, customers.toList())\n\nval shop = shop(\"jb test shop\",\n        customer(lucas, Canberra,\n                order(reSharper),\n                order(reSharper, dotMemory, dotTrace)\n        ),\n        customer(cooper, Canberra),\n        customer(nathan, Vancouver,\n                order(rubyMine, webStorm)\n        ),\n        customer(reka, Budapest,\n                order(idea, isDelivered = false),\n                order(idea, isDelivered = false),\n                order(idea)\n        ),\n        customer(bajram, Ankara,\n                order(reSharper)\n        ),\n        customer(asuka, Tokyo,\n                order(idea)\n        ),\n        customer(riku, Tokyo,\n                order(phpStorm, phpStorm),\n                order(phpStorm)\n        )\n\n)\n\nval customers: Map<String, Customer> = shop.customers.fold(hashMapOf<String, Customer>(), {\n        map, customer ->\n        map[customer.name] = customer\n        map\n})\n\nval orderedProducts = setOf(idea, reSharper, dotTrace, dotMemory, rubyMine, webStorm, phpStorm)\n\nval sortedCustomers = listOf(cooper, nathan, bajram, asuka, lucas, riku, reka).map { customers[it] }\n\nval groupedByCities = mapOf(\n        Canberra to listOf(lucas, cooper),\n        Vancouver to listOf(nathan),\n        Budapest to listOf(reka),\n        Ankara to listOf(bajram),\n        Tokyo to listOf(asuka, riku)\n).mapValues { it.value.map { name -> customers[name] } }",
      "data class Shop(val name: String, val customers: List<Customer>)\n\ndata class Customer(val name: String, val city: City, val orders: List<Order>) {\n    override fun toString() = \"\$name from \${city.name}\"\n}\n\ndata class Order(val products: List<Product>, val isDelivered: Boolean)\n\ndata class Product(val name: String, val price: Double) {\n    override fun toString() = \"'\$name' for \$price\"\n}\n\ndata class City(val name: String) {\n    override fun toString() = name\n}"
    )
  }

  @ParameterizedTest
  @EnumSource(ExecutorMode::class)
  fun `koans test sum`(mode: ExecutorMode) {
    runKoanTest(
      mode,
      "// Return the sum of prices of all products that a customer has ordered.\n// Note: the customer may order the same product for several times.\nfun Customer.getTotalOrderPrice(): Double = orders.flatMap { it.products }.sumByDouble { it.price }",
      "import org.junit.Assert\nimport org.junit.Test\nimport koans.util.toMessage\n\nclass TestSum {\n    @Test fun testGetTotalOrderPrice() {\n        Assert.assertEquals(\"getTotalOrderPrice\".toMessage(),\n                148.0, customers[nathan]!!.getTotalOrderPrice(), 0.001)\n    }\n\n    @Test fun testTotalPriceForRepeatedProducts() {\n        Assert.assertEquals(\"getTotalOrderPrice\".toMessage(),\n                586.0, customers[lucas]!!.getTotalOrderPrice(), 0.001)\n    }\n}",
      "//products\nval idea = Product(\"IntelliJ IDEA Ultimate\", 199.0)\nval reSharper = Product(\"ReSharper\", 149.0)\nval dotTrace = Product(\"DotTrace\", 159.0)\nval dotMemory = Product(\"DotTrace\", 129.0)\nval dotCover = Product(\"DotCover\", 99.0)\nval appCode = Product(\"AppCode\", 99.0)\nval phpStorm = Product(\"PhpStorm\", 99.0)\nval pyCharm = Product(\"PyCharm\", 99.0)\nval rubyMine = Product(\"RubyMine\", 99.0)\nval webStorm = Product(\"WebStorm\", 49.0)\nval teamCity = Product(\"TeamCity\", 299.0)\nval youTrack = Product(\"YouTrack\", 500.0)\n\n//customers\nval lucas = \"Lucas\"\nval cooper = \"Cooper\"\nval nathan = \"Nathan\"\nval reka = \"Reka\"\nval bajram = \"Bajram\"\nval asuka = \"Asuka\"\nval riku = \"Riku\"\n\n//cities\nval Canberra = City(\"Canberra\")\nval Vancouver = City(\"Vancouver\")\nval Budapest = City(\"Budapest\")\nval Ankara = City(\"Ankara\")\nval Tokyo = City(\"Tokyo\")\n\nfun customer(name: String, city: City, vararg orders: Order) = Customer(name, city, orders.toList())\nfun order(vararg products: Product, isDelivered: Boolean = true) = Order(products.toList(), isDelivered)\nfun shop(name: String, vararg customers: Customer) = Shop(name, customers.toList())\n\nval shop = shop(\"jb test shop\",\n        customer(lucas, Canberra,\n                order(reSharper),\n                order(reSharper, dotMemory, dotTrace)\n        ),\n        customer(cooper, Canberra),\n        customer(nathan, Vancouver,\n                order(rubyMine, webStorm)\n        ),\n        customer(reka, Budapest,\n                order(idea, isDelivered = false),\n                order(idea, isDelivered = false),\n                order(idea)\n        ),\n        customer(bajram, Ankara,\n                order(reSharper)\n        ),\n        customer(asuka, Tokyo,\n                order(idea)\n        ),\n        customer(riku, Tokyo,\n                order(phpStorm, phpStorm),\n                order(phpStorm)\n        )\n\n)\n\nval customers: Map<String, Customer> = shop.customers.fold(hashMapOf<String, Customer>(), {\n        map, customer ->\n        map[customer.name] = customer\n        map\n})\n\nval orderedProducts = setOf(idea, reSharper, dotTrace, dotMemory, rubyMine, webStorm, phpStorm)\n\nval sortedCustomers = listOf(cooper, nathan, bajram, asuka, lucas, riku, reka).map { customers[it] }\n\nval groupedByCities = mapOf(\n        Canberra to listOf(lucas, cooper),\n        Vancouver to listOf(nathan),\n        Budapest to listOf(reka),\n        Ankara to listOf(bajram),\n        Tokyo to listOf(asuka, riku)\n).mapValues { it.value.map { name -> customers[name] } }",
      "data class Shop(val name: String, val customers: List<Customer>)\n\ndata class Customer(val name: String, val city: City, val orders: List<Order>) {\n    override fun toString() = \"\$name from \${city.name}\"\n}\n\ndata class Order(val products: List<Product>, val isDelivered: Boolean)\n\ndata class Product(val name: String, val price: Double) {\n    override fun toString() = \"'\$name' for \$price\"\n}\n\ndata class City(val name: String) {\n    override fun toString() = name\n}"
    )
  }

  @ParameterizedTest
  @EnumSource(ExecutorMode::class)
  fun `koans test GroupBy`(mode: ExecutorMode) {
    runKoanTest(
      mode,
      "// Return a map of the customers living in each city\nfun Shop.groupCustomersByCity(): Map<City, List<Customer>> = customers.groupBy { it.city }",
      "import org.junit.Assert\nimport org.junit.Test\nimport koans.util.toMessage\n\nclass TestGroupBy {\n    @Test fun testGroupCustomersByCity() {\n        Assert.assertEquals(\"groupCustomersByCity\".toMessage(),\n                groupedByCities, shop.groupCustomersByCity())\n    }\n}",
      "//products\nval idea = Product(\"IntelliJ IDEA Ultimate\", 199.0)\nval reSharper = Product(\"ReSharper\", 149.0)\nval dotTrace = Product(\"DotTrace\", 159.0)\nval dotMemory = Product(\"DotTrace\", 129.0)\nval dotCover = Product(\"DotCover\", 99.0)\nval appCode = Product(\"AppCode\", 99.0)\nval phpStorm = Product(\"PhpStorm\", 99.0)\nval pyCharm = Product(\"PyCharm\", 99.0)\nval rubyMine = Product(\"RubyMine\", 99.0)\nval webStorm = Product(\"WebStorm\", 49.0)\nval teamCity = Product(\"TeamCity\", 299.0)\nval youTrack = Product(\"YouTrack\", 500.0)\n\n//customers\nval lucas = \"Lucas\"\nval cooper = \"Cooper\"\nval nathan = \"Nathan\"\nval reka = \"Reka\"\nval bajram = \"Bajram\"\nval asuka = \"Asuka\"\nval riku = \"Riku\"\n\n//cities\nval Canberra = City(\"Canberra\")\nval Vancouver = City(\"Vancouver\")\nval Budapest = City(\"Budapest\")\nval Ankara = City(\"Ankara\")\nval Tokyo = City(\"Tokyo\")\n\nfun customer(name: String, city: City, vararg orders: Order) = Customer(name, city, orders.toList())\nfun order(vararg products: Product, isDelivered: Boolean = true) = Order(products.toList(), isDelivered)\nfun shop(name: String, vararg customers: Customer) = Shop(name, customers.toList())\n\nval shop = shop(\"jb test shop\",\n        customer(lucas, Canberra,\n                order(reSharper),\n                order(reSharper, dotMemory, dotTrace)\n        ),\n        customer(cooper, Canberra),\n        customer(nathan, Vancouver,\n                order(rubyMine, webStorm)\n        ),\n        customer(reka, Budapest,\n                order(idea, isDelivered = false),\n                order(idea, isDelivered = false),\n                order(idea)\n        ),\n        customer(bajram, Ankara,\n                order(reSharper)\n        ),\n        customer(asuka, Tokyo,\n                order(idea)\n        ),\n        customer(riku, Tokyo,\n                order(phpStorm, phpStorm),\n                order(phpStorm)\n        )\n\n)\n\nval customers: Map<String, Customer> = shop.customers.fold(hashMapOf<String, Customer>(), {\n        map, customer ->\n        map[customer.name] = customer\n        map\n})\n\nval orderedProducts = setOf(idea, reSharper, dotTrace, dotMemory, rubyMine, webStorm, phpStorm)\n\nval sortedCustomers = listOf(cooper, nathan, bajram, asuka, lucas, riku, reka).map { customers[it] }\n\nval groupedByCities = mapOf(\n        Canberra to listOf(lucas, cooper),\n        Vancouver to listOf(nathan),\n        Budapest to listOf(reka),\n        Ankara to listOf(bajram),\n        Tokyo to listOf(asuka, riku)\n).mapValues { it.value.map { name -> customers[name] } }",
      "data class Shop(val name: String, val customers: List<Customer>)\n\ndata class Customer(val name: String, val city: City, val orders: List<Order>) {\n    override fun toString() = \"\$name from \${city.name}\"\n}\n\ndata class Order(val products: List<Product>, val isDelivered: Boolean)\n\ndata class Product(val name: String, val price: Double) {\n    override fun toString() = \"'\$name' for \$price\"\n}\n\ndata class City(val name: String) {\n    override fun toString() = name\n}"
    )
  }

  @ParameterizedTest
  @EnumSource(ExecutorMode::class)
  fun `koans test Partition`(mode: ExecutorMode) {
    runKoanTest(
      mode,
      "// Return customers who have more undelivered orders than delivered\nfun Shop.getCustomersWithMoreUndeliveredOrdersThanDelivered(): Set<Customer> = customers.filter {\n    val (delivered, undelivered) = it.orders.partition { it.isDelivered }\n    undelivered.size > delivered.size\n}.toSet()",
      "import org.junit.Assert\nimport org.junit.Test\nimport koans.util.toMessage\n\nclass TestPartition {\n    @Test fun testGetCustomersWhoHaveMoreUndeliveredOrdersThanDelivered() {\n        Assert.assertEquals(\"getCustomerWithMaximumNumberOfOrders\".toMessage(),\n                setOf(customers[reka]), shop.getCustomersWithMoreUndeliveredOrdersThanDelivered())\n    }\n}",
      "//products\nval idea = Product(\"IntelliJ IDEA Ultimate\", 199.0)\nval reSharper = Product(\"ReSharper\", 149.0)\nval dotTrace = Product(\"DotTrace\", 159.0)\nval dotMemory = Product(\"DotTrace\", 129.0)\nval dotCover = Product(\"DotCover\", 99.0)\nval appCode = Product(\"AppCode\", 99.0)\nval phpStorm = Product(\"PhpStorm\", 99.0)\nval pyCharm = Product(\"PyCharm\", 99.0)\nval rubyMine = Product(\"RubyMine\", 99.0)\nval webStorm = Product(\"WebStorm\", 49.0)\nval teamCity = Product(\"TeamCity\", 299.0)\nval youTrack = Product(\"YouTrack\", 500.0)\n\n//customers\nval lucas = \"Lucas\"\nval cooper = \"Cooper\"\nval nathan = \"Nathan\"\nval reka = \"Reka\"\nval bajram = \"Bajram\"\nval asuka = \"Asuka\"\nval riku = \"Riku\"\n\n//cities\nval Canberra = City(\"Canberra\")\nval Vancouver = City(\"Vancouver\")\nval Budapest = City(\"Budapest\")\nval Ankara = City(\"Ankara\")\nval Tokyo = City(\"Tokyo\")\n\nfun customer(name: String, city: City, vararg orders: Order) = Customer(name, city, orders.toList())\nfun order(vararg products: Product, isDelivered: Boolean = true) = Order(products.toList(), isDelivered)\nfun shop(name: String, vararg customers: Customer) = Shop(name, customers.toList())\n\nval shop = shop(\"jb test shop\",\n        customer(lucas, Canberra,\n                order(reSharper),\n                order(reSharper, dotMemory, dotTrace)\n        ),\n        customer(cooper, Canberra),\n        customer(nathan, Vancouver,\n                order(rubyMine, webStorm)\n        ),\n        customer(reka, Budapest,\n                order(idea, isDelivered = false),\n                order(idea, isDelivered = false),\n                order(idea)\n        ),\n        customer(bajram, Ankara,\n                order(reSharper)\n        ),\n        customer(asuka, Tokyo,\n                order(idea)\n        ),\n        customer(riku, Tokyo,\n                order(phpStorm, phpStorm),\n                order(phpStorm)\n        )\n\n)\n\nval customers: Map<String, Customer> = shop.customers.fold(hashMapOf<String, Customer>(), {\n        map, customer ->\n        map[customer.name] = customer\n        map\n})\n\nval orderedProducts = setOf(idea, reSharper, dotTrace, dotMemory, rubyMine, webStorm, phpStorm)\n\nval sortedCustomers = listOf(cooper, nathan, bajram, asuka, lucas, riku, reka).map { customers[it] }\n\nval groupedByCities = mapOf(\n        Canberra to listOf(lucas, cooper),\n        Vancouver to listOf(nathan),\n        Budapest to listOf(reka),\n        Ankara to listOf(bajram),\n        Tokyo to listOf(asuka, riku)\n).mapValues { it.value.map { name -> customers[name] } }",
      "data class Shop(val name: String, val customers: List<Customer>)\n\ndata class Customer(val name: String, val city: City, val orders: List<Order>) {\n    override fun toString() = \"\$name from \${city.name}\"\n}\n\ndata class Order(val products: List<Product>, val isDelivered: Boolean)\n\ndata class Product(val name: String, val price: Double) {\n    override fun toString() = \"'\$name' for \$price\"\n}\n\ndata class City(val name: String) {\n    override fun toString() = name\n}"
    )
  }

  @ParameterizedTest
  @EnumSource(ExecutorMode::class)
  fun `koans test Fold`(mode: ExecutorMode) {
    runKoanTest(
      mode,
      "// Return the set of products that were ordered by every customer\nfun Shop.getSetOfProductsOrderedByEveryCustomer(): Set<Product> {\n    val allProducts = customers.flatMap { it.orders.flatMap { it.products }}.toSet()\n    return customers.fold(allProducts, {\n        orderedByAll, customer ->\n        orderedByAll.intersect(customer.orders.flatMap { it.products }.toSet())\n    })\n}",
      "import org.junit.Assert\nimport org.junit.Test\nimport koans.util.toMessage\n\nclass TestFold {\n    @Test fun testGetProductsOrderedByAllCustomers() {\n        val testShop = shop(\"test shop for 'fold'\",\n                customer(lucas, Canberra,\n                        order(idea),\n                        order(webStorm)\n                ),\n                customer(reka, Budapest,\n                        order(idea),\n                        order(youTrack)\n                )\n        )\n        Assert.assertEquals(\"getSetOfProductsOrderedByEveryCustomer\".toMessage(),\n                setOf(idea), testShop.getSetOfProductsOrderedByEveryCustomer())\n\n    }\n}",
      "//products\nval idea = Product(\"IntelliJ IDEA Ultimate\", 199.0)\nval reSharper = Product(\"ReSharper\", 149.0)\nval dotTrace = Product(\"DotTrace\", 159.0)\nval dotMemory = Product(\"DotTrace\", 129.0)\nval dotCover = Product(\"DotCover\", 99.0)\nval appCode = Product(\"AppCode\", 99.0)\nval phpStorm = Product(\"PhpStorm\", 99.0)\nval pyCharm = Product(\"PyCharm\", 99.0)\nval rubyMine = Product(\"RubyMine\", 99.0)\nval webStorm = Product(\"WebStorm\", 49.0)\nval teamCity = Product(\"TeamCity\", 299.0)\nval youTrack = Product(\"YouTrack\", 500.0)\n\n//customers\nval lucas = \"Lucas\"\nval cooper = \"Cooper\"\nval nathan = \"Nathan\"\nval reka = \"Reka\"\nval bajram = \"Bajram\"\nval asuka = \"Asuka\"\nval riku = \"Riku\"\n\n//cities\nval Canberra = City(\"Canberra\")\nval Vancouver = City(\"Vancouver\")\nval Budapest = City(\"Budapest\")\nval Ankara = City(\"Ankara\")\nval Tokyo = City(\"Tokyo\")\n\nfun customer(name: String, city: City, vararg orders: Order) = Customer(name, city, orders.toList())\nfun order(vararg products: Product, isDelivered: Boolean = true) = Order(products.toList(), isDelivered)\nfun shop(name: String, vararg customers: Customer) = Shop(name, customers.toList())\n\nval shop = shop(\"jb test shop\",\n        customer(lucas, Canberra,\n                order(reSharper),\n                order(reSharper, dotMemory, dotTrace)\n        ),\n        customer(cooper, Canberra),\n        customer(nathan, Vancouver,\n                order(rubyMine, webStorm)\n        ),\n        customer(reka, Budapest,\n                order(idea, isDelivered = false),\n                order(idea, isDelivered = false),\n                order(idea)\n        ),\n        customer(bajram, Ankara,\n                order(reSharper)\n        ),\n        customer(asuka, Tokyo,\n                order(idea)\n        ),\n        customer(riku, Tokyo,\n                order(phpStorm, phpStorm),\n                order(phpStorm)\n        )\n\n)\n\nval customers: Map<String, Customer> = shop.customers.fold(hashMapOf<String, Customer>(), {\n        map, customer ->\n        map[customer.name] = customer\n        map\n})\n\nval orderedProducts = setOf(idea, reSharper, dotTrace, dotMemory, rubyMine, webStorm, phpStorm)\n\nval sortedCustomers = listOf(cooper, nathan, bajram, asuka, lucas, riku, reka).map { customers[it] }\n\nval groupedByCities = mapOf(\n        Canberra to listOf(lucas, cooper),\n        Vancouver to listOf(nathan),\n        Budapest to listOf(reka),\n        Ankara to listOf(bajram),\n        Tokyo to listOf(asuka, riku)\n).mapValues { it.value.map { name -> customers[name] } }",
      "data class Shop(val name: String, val customers: List<Customer>)\n\ndata class Customer(val name: String, val city: City, val orders: List<Order>) {\n    override fun toString() = \"\$name from \${city.name}\"\n}\n\ndata class Order(val products: List<Product>, val isDelivered: Boolean)\n\ndata class Product(val name: String, val price: Double) {\n    override fun toString() = \"'\$name' for \$price\"\n}\n\ndata class City(val name: String) {\n    override fun toString() = name\n}"
    )
  }

  @ParameterizedTest
  @EnumSource(ExecutorMode::class)
  fun `koans test Compound tasks`(mode: ExecutorMode) {
    runKoanTest(
      mode,
      "// Return the most expensive product among all delivered products\n// (use the Order.isDelivered flag)\nfun Customer.getMostExpensiveDeliveredProduct(): Product? {\n    return orders.filter { it.isDelivered }.flatMap { it.products }.maxBy { it.price }\n}\n\n// Return how many times the given product was ordered.\n// Note: a customer may order the same product for several times.\nfun Shop.getNumberOfTimesProductWasOrdered(product: Product): Int {\n    return customers.flatMap { it.getOrderedProductsList() }.count { it == product }\n}\n\nfun Customer.getOrderedProductsList(): List<Product> {\n    return orders.flatMap { it.products }\n}",
      "import org.junit.Assert\nimport org.junit.Test\nimport koans.util.toMessage\n\nclass K_Compound_Tasks {\n\n    @Test fun testMostExpensiveDeliveredProduct() {\n        val testShop = shop(\"test shop for 'most expensive delivered product'\",\n                customer(lucas, Canberra,\n                        order(idea, isDelivered = false),\n                        order(reSharper)\n                )\n        )\n        Assert.assertEquals(\"getMostExpensiveDeliveredProduct\".toMessage(), reSharper, testShop.customers[0].getMostExpensiveDeliveredProduct())\n    }\n\n    @Test fun testNumberOfTimesEachProductWasOrdered() {\n        Assert.assertEquals(4, shop.getNumberOfTimesProductWasOrdered(idea))\n    }\n\n    @Test fun testNumberOfTimesEachProductWasOrderedForRepeatedProduct() {\n        Assert.assertEquals(\"A customer may order a product for several times\",\n                3, shop.getNumberOfTimesProductWasOrdered(reSharper))\n    }\n\n    @Test fun testNumberOfTimesEachProductWasOrderedForRepeatedInOrderProduct() {\n        Assert.assertEquals(\"An order may contain a particular product more than once\",\n                3, shop.getNumberOfTimesProductWasOrdered(phpStorm))\n    }\n}",
      "//products\nval idea = Product(\"IntelliJ IDEA Ultimate\", 199.0)\nval reSharper = Product(\"ReSharper\", 149.0)\nval dotTrace = Product(\"DotTrace\", 159.0)\nval dotMemory = Product(\"DotTrace\", 129.0)\nval dotCover = Product(\"DotCover\", 99.0)\nval appCode = Product(\"AppCode\", 99.0)\nval phpStorm = Product(\"PhpStorm\", 99.0)\nval pyCharm = Product(\"PyCharm\", 99.0)\nval rubyMine = Product(\"RubyMine\", 99.0)\nval webStorm = Product(\"WebStorm\", 49.0)\nval teamCity = Product(\"TeamCity\", 299.0)\nval youTrack = Product(\"YouTrack\", 500.0)\n\n//customers\nval lucas = \"Lucas\"\nval cooper = \"Cooper\"\nval nathan = \"Nathan\"\nval reka = \"Reka\"\nval bajram = \"Bajram\"\nval asuka = \"Asuka\"\nval riku = \"Riku\"\n\n//cities\nval Canberra = City(\"Canberra\")\nval Vancouver = City(\"Vancouver\")\nval Budapest = City(\"Budapest\")\nval Ankara = City(\"Ankara\")\nval Tokyo = City(\"Tokyo\")\n\nfun customer(name: String, city: City, vararg orders: Order) = Customer(name, city, orders.toList())\nfun order(vararg products: Product, isDelivered: Boolean = true) = Order(products.toList(), isDelivered)\nfun shop(name: String, vararg customers: Customer) = Shop(name, customers.toList())\n\nval shop = shop(\"jb test shop\",\n        customer(lucas, Canberra,\n                order(reSharper),\n                order(reSharper, dotMemory, dotTrace)\n        ),\n        customer(cooper, Canberra),\n        customer(nathan, Vancouver,\n                order(rubyMine, webStorm)\n        ),\n        customer(reka, Budapest,\n                order(idea, isDelivered = false),\n                order(idea, isDelivered = false),\n                order(idea)\n        ),\n        customer(bajram, Ankara,\n                order(reSharper)\n        ),\n        customer(asuka, Tokyo,\n                order(idea)\n        ),\n        customer(riku, Tokyo,\n                order(phpStorm, phpStorm),\n                order(phpStorm)\n        )\n\n)\n\nval customers: Map<String, Customer> = shop.customers.fold(hashMapOf<String, Customer>(), {\n        map, customer ->\n        map[customer.name] = customer\n        map\n})\n\nval orderedProducts = setOf(idea, reSharper, dotTrace, dotMemory, rubyMine, webStorm, phpStorm)\n\nval sortedCustomers = listOf(cooper, nathan, bajram, asuka, lucas, riku, reka).map { customers[it] }\n\nval groupedByCities = mapOf(\n        Canberra to listOf(lucas, cooper),\n        Vancouver to listOf(nathan),\n        Budapest to listOf(reka),\n        Ankara to listOf(bajram),\n        Tokyo to listOf(asuka, riku)\n).mapValues { it.value.map { name -> customers[name] } }",
      "data class Shop(val name: String, val customers: List<Customer>)\n\ndata class Customer(val name: String, val city: City, val orders: List<Order>) {\n    override fun toString() = \"\$name from \${city.name}\"\n}\n\ndata class Order(val products: List<Product>, val isDelivered: Boolean)\n\ndata class Product(val name: String, val price: Double) {\n    override fun toString() = \"'\$name' for \$price\"\n}\n\ndata class City(val name: String) {\n    override fun toString() = name\n}"
    )
  }

  @ParameterizedTest
  @EnumSource(ExecutorMode::class)
  fun `koans test Get used to new style`(mode: ExecutorMode) {
    runKoanTest(
      mode,
      "fun doSomethingStrangeWithCollection(collection: Collection<String>): Collection<String>? {\n\n    val groupsByLength = collection. groupBy { s -> s.length }\n\n    val maximumSizeOfGroup = groupsByLength.values.map { group -> group.size }.max()\n\n    return groupsByLength.values.firstOrNull { group -> group.size == maximumSizeOfGroup }\n}",
      "import org.junit.Test\nimport org.junit.Assert\nimport koans.util.inEquals\n\nclass TestExtensionsOnCollections {\n    @Test fun testCollectionOfOneElement() {\n        doTest(listOf(\"a\"), listOf(\"a\"))\n    }\n\n    @Test fun testSimpleCollection() {\n        doTest(listOf(\"a\", \"c\"), listOf(\"a\", \"bb\", \"c\"))\n    }\n\n    @Test fun testCollectionWithEmptyStrings() {\n        doTest(listOf(\"\", \"\", \"\", \"\"), listOf(\"\", \"\", \"\", \"\", \"a\", \"bb\", \"ccc\", \"dddd\"))\n    }\n\n    @Test fun testCollectionWithTwoGroupsOfMaximalSize() {\n        doTest(listOf(\"a\", \"c\"), listOf(\"a\", \"bb\", \"c\", \"dd\"))\n    }\n\n    private fun doTest(expected: Collection<String>?, argument: Collection<String>) {\n        Assert.assertEquals(\"The function 'doSomethingStrangeWithCollection' should do at least something with a collection\".inEquals(),\n                expected, doSomethingStrangeWithCollection(argument))\n    }\n}",
      "//products\nval idea = Product(\"IntelliJ IDEA Ultimate\", 199.0)\nval reSharper = Product(\"ReSharper\", 149.0)\nval dotTrace = Product(\"DotTrace\", 159.0)\nval dotMemory = Product(\"DotTrace\", 129.0)\nval dotCover = Product(\"DotCover\", 99.0)\nval appCode = Product(\"AppCode\", 99.0)\nval phpStorm = Product(\"PhpStorm\", 99.0)\nval pyCharm = Product(\"PyCharm\", 99.0)\nval rubyMine = Product(\"RubyMine\", 99.0)\nval webStorm = Product(\"WebStorm\", 49.0)\nval teamCity = Product(\"TeamCity\", 299.0)\nval youTrack = Product(\"YouTrack\", 500.0)\n\n//customers\nval lucas = \"Lucas\"\nval cooper = \"Cooper\"\nval nathan = \"Nathan\"\nval reka = \"Reka\"\nval bajram = \"Bajram\"\nval asuka = \"Asuka\"\nval riku = \"Riku\"\n\n//cities\nval Canberra = City(\"Canberra\")\nval Vancouver = City(\"Vancouver\")\nval Budapest = City(\"Budapest\")\nval Ankara = City(\"Ankara\")\nval Tokyo = City(\"Tokyo\")\n\nfun customer(name: String, city: City, vararg orders: Order) = Customer(name, city, orders.toList())\nfun order(vararg products: Product, isDelivered: Boolean = true) = Order(products.toList(), isDelivered)\nfun shop(name: String, vararg customers: Customer) = Shop(name, customers.toList())\n\nval shop = shop(\"jb test shop\",\n        customer(lucas, Canberra,\n                order(reSharper),\n                order(reSharper, dotMemory, dotTrace)\n        ),\n        customer(cooper, Canberra),\n        customer(nathan, Vancouver,\n                order(rubyMine, webStorm)\n        ),\n        customer(reka, Budapest,\n                order(idea, isDelivered = false),\n                order(idea, isDelivered = false),\n                order(idea)\n        ),\n        customer(bajram, Ankara,\n                order(reSharper)\n        ),\n        customer(asuka, Tokyo,\n                order(idea)\n        ),\n        customer(riku, Tokyo,\n                order(phpStorm, phpStorm),\n                order(phpStorm)\n        )\n\n)\n\nval customers: Map<String, Customer> = shop.customers.fold(hashMapOf<String, Customer>(), {\n        map, customer ->\n        map[customer.name] = customer\n        map\n})\n\nval orderedProducts = setOf(idea, reSharper, dotTrace, dotMemory, rubyMine, webStorm, phpStorm)\n\nval sortedCustomers = listOf(cooper, nathan, bajram, asuka, lucas, riku, reka).map { customers[it] }\n\nval groupedByCities = mapOf(\n        Canberra to listOf(lucas, cooper),\n        Vancouver to listOf(nathan),\n        Budapest to listOf(reka),\n        Ankara to listOf(bajram),\n        Tokyo to listOf(asuka, riku)\n).mapValues { it.value.map { name -> customers[name] } }",
      "data class Shop(val name: String, val customers: List<Customer>)\n\ndata class Customer(val name: String, val city: City, val orders: List<Order>) {\n    override fun toString() = \"\$name from \${city.name}\"\n}\n\ndata class Order(val products: List<Product>, val isDelivered: Boolean)\n\ndata class Product(val name: String, val price: Double) {\n    override fun toString() = \"'\$name' for \$price\"\n}\n\ndata class City(val name: String) {\n    override fun toString() = name\n}"
    )
  }

  @ParameterizedTest
  @EnumSource(ExecutorMode::class)
  fun `koans test Properties`(mode: ExecutorMode) {
    runKoanTest(
      mode,
      "class PropertyExample() {\n    var counter = 0\n    var propertyWithCounter: Int? = null\n        set(v: Int?) {\n            field = v\n            counter++\n        }\n}",
      "import org.junit.Assert\nimport org.junit.Test\n\nclass TestProperties {\n    @Test fun testPropertyWithCounter() {\n        val q = PropertyExample()\n        q.propertyWithCounter = 14\n        q.propertyWithCounter = 21\n        q.propertyWithCounter = 32\n        Assert.assertTrue(\"The property 'changeCounter' should contain the number of assignments to 'propertyWithCounter'\",\n                3 == q.counter)\n        // Here we have to use !! due to false smart cast impossible\n        Assert.assertTrue(\"The property 'propertyWithCounter' should be set\", 32 == q.propertyWithCounter!!)\n    }\n\n}"
    )
  }

  @ParameterizedTest
  @EnumSource(ExecutorMode::class)
  fun `koans test Lazy property`(mode: ExecutorMode) {
    runKoanTest(
      mode,
      "class LazyProperty(val initializer: () -> Int) {\n    var value: Int? = null\n    val lazy: Int\n        get() {\n            if (value == null) {\n                value = initializer()\n            }\n            return value!!\n        }\n}",
      "import org.junit.Assert\nimport org.junit.Test\n\nclass TetLazyProperty {\n    @Test fun testLazy() {\n        var initialized = false\n        val lazyProperty = LazyProperty({ initialized = true; 42 })\n        Assert.assertFalse(\"Property shouldn't be initialized before access\", initialized)\n        val result: Int = lazyProperty.lazy\n        Assert.assertTrue(\"Property should be initialized after access\", initialized)\n        Assert.assertEquals(42, result)\n    }\n\n    @Test fun initializedOnce() {\n        var initialized = 0\n        val lazyProperty = LazyProperty( { initialized++; 42 })\n        lazyProperty.lazy\n        lazyProperty.lazy\n        Assert.assertEquals(\"Lazy property should be initialized once\", 1, initialized)\n\n    }\n}"
    )
  }

  @ParameterizedTest
  @EnumSource(ExecutorMode::class)
  fun `koans test Delegates examples`(mode: ExecutorMode) {
    runKoanTest(
      mode,
      "class LazyProperty(val initializer: () -> Int) {\n    val lazyValue: Int by lazy(initializer)\n}",
      "import org.junit.Assert\nimport org.junit.Test\nimport java.util.HashMap\n\nclass TestDelegatesExamples {\n    @Test fun testLazy() {\n        var initialized = false\n        val lazyProperty = LazyProperty({ initialized = true; 42 })\n        Assert.assertFalse(\"Property shouldn't be initialized before access\", initialized)\n        val result: Int = lazyProperty.lazyValue\n        Assert.assertTrue(\"Property should be initialized after access\", initialized)\n        Assert.assertEquals(42, result)\n    }\n\n    @Test fun initializedOnce() {\n        var initialized = 0\n        val lazyProperty = LazyProperty( { initialized++; 42 })\n        lazyProperty.lazyValue\n        lazyProperty.lazyValue\n        Assert.assertEquals(\"Lazy property should be initialized once\", 1, initialized)\n    }\n}"
    )
  }

  @ParameterizedTest
  @EnumSource(ExecutorMode::class)
  fun `koans test Delegates how it works`(mode: ExecutorMode) {
    runKoanTest(
      mode,
      "import kotlin.properties.ReadWriteProperty\nimport kotlin.reflect.KProperty\n\nclass D {\n    var date: MyDate by EffectiveDate()\n}\n\nclass EffectiveDate<R> : ReadWriteProperty<R, MyDate> {\n\n    var timeInMillis: Long? = null\n\n    override fun getValue(thisRef: R, property: KProperty<*>): MyDate {\n        return timeInMillis!!.toDate()\n    }\n\n    override fun setValue(thisRef: R, property: KProperty<*>, value: MyDate) {\n        timeInMillis = value.toMillis()\n    }\n}",
      "import org.junit.Assert\nimport org.junit.Test\n\nclass TestDelegatesHowItWorks {\n    @Test fun testDate() {\n        val d = D()\n        d.date = MyDate(2014, 1, 13)\n        val message = \"The methods 'getValue' and 'setValue' are implemented incorrectly\"\n        Assert.assertTrue(message, 2014 == d.date.year)\n        Assert.assertTrue(message, 1 == d.date.month)\n        Assert.assertTrue(message, 13 == d.date.dayOfMonth)\n    }\n}",
      "import java.util.Calendar\n\ndata class MyDate(val year: Int, val month: Int, val dayOfMonth: Int)\n\nfun MyDate.toMillis(): Long {\n    val c = Calendar.getInstance()\n    c.set(year, month, dayOfMonth, 0, 0, 0)\n    c.set(Calendar.MILLISECOND, 0)\n    return c.getTimeInMillis()\n}\n\nfun Long.toDate(): MyDate {\n    val c = Calendar.getInstance()\n    c.setTimeInMillis(this)\n    return MyDate(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DATE))\n}"
    )
  }

  @ParameterizedTest
  @EnumSource(ExecutorMode::class)
  fun `koans test Function literals with receiver`(mode: ExecutorMode) {
    runKoanTest(
      mode,
      "fun task(): List<Boolean> {\n    val isEven: Int.() -> Boolean = { this % 2 == 0 }\n    val isOdd: Int.() -> Boolean = { this % 2 != 0 }\n\n    return listOf(42.isOdd(), 239.isOdd(), 294823098.isEven())\n}",
      "import org.junit.Assert\nimport org.junit.Test\nimport koans.util.inEquals\n\nclass TestExtensionFunctionLiterals {\n    @Test fun testIsOddAndIsEven() {\n        Assert.assertEquals(\"The functions 'isOdd' and 'isEven' should be implemented correctly\".inEquals(),\n                listOf(false, true, true), task())\n\n    }\n}"
    )
  }

  @ParameterizedTest
  @EnumSource(ExecutorMode::class)
  fun `koans test -String and map builders`(mode: ExecutorMode) {
    runKoanTest(
      mode,
      "import java.util.HashMap\n\nfun <K, V> buildMap(build: HashMap<K, V>.() -> Unit): Map<K, V> {\n    val map = HashMap<K, V>()\n    map.build()\n    return map\n}\n\nfun usage(): Map<Int, String> {\n    return buildMap {\n        put(0, \"0\")\n        for (i in 1..10) {\n            put(i, \"\$i\")\n        }\n    }\n}",
      "import org.junit.Assert\nimport org.junit.Test\nimport koans.util.inEquals\n\nclass TestStringAndMapBuilders {\n    @Test fun testBuildMap() {\n        val map: Map<Int, String> = buildMap {\n            put(0, \"0\")\n            for (i in 1..10) {\n                put(i, \"\$i\")\n            }\n        }\n        val expected = hashMapOf<Int, String>()\n        for (i in 0..10) {\n            expected[i] = \"\$i\"\n        }\n        Assert.assertEquals(\"Map should be filled with the right values\".inEquals(), expected, map)\n    }\n}"
    )
  }

  @ParameterizedTest
  @EnumSource(ExecutorMode::class)
  fun `koans test The function apply`(mode: ExecutorMode) {
    runKoanTest(
      mode,
      "fun <T> T.myApply(f: T.() -> Unit): T { f(); return this }\n\nfun createString(): String {\n    return StringBuilder().myApply {\n        append(\"Numbers: \")\n        for (i in 1..10) {\n            append(i)\n        }\n    }.toString()\n}\n\nfun createMap(): Map<Int, String> {\n    return hashMapOf<Int, String>().myApply {\n        put(0, \"0\")\n        for (i in 1..10) {\n            put(i, \"\$i\")\n        }\n    }\n}",
      "import org.junit.Assert\nimport org.junit.Test\nimport java.util.HashMap\nimport koans.util.inEquals\n\nclass TestTheFunctionWith {\n    @Test fun testCreateString() {\n        val s = createString()\n        val sb = StringBuilder()\n        sb.append(\"Numbers: \")\n        for (i in 1..10) {\n            sb.append(i)\n        }\n        Assert.assertEquals(\"String should be built\".inEquals(), sb.toString(), s)\n    }\n\n    @Test fun testCreateMap() {\n        val map = createMap()\n        val expected = HashMap<Int, String>()\n        for (i in 0..10) {\n            expected[i] = \"\$i\"\n        }\n        Assert.assertEquals(\"Map should be filled with the right values\".inEquals(), expected, map)\n    }\n}"
    )
  }

  @ParameterizedTest
  @EnumSource(ExecutorMode::class)
  fun `koans test Builders how it works`(mode: ExecutorMode) {
    runKoanTest(
      mode,
      "import Answer.*\n\nenum class Answer { a, b, c }\n\nval answers = mapOf<Int, Answer?>(\n        1 to c, 2 to b, 3 to b, 4 to c\n)",
      "import org.junit.Test\nimport org.junit.Assert\nimport Answer.*\n\nclass TestBuildersHowItWorks {\n    @Test fun testBuildersQuiz() {\n        if (answers.values.toSet() == setOf(null)) {\n            Assert.fail(\"Please specify your answers!\")\n        }\n        val correctAnswers = mapOf(22 - 20 to b, 1 + 3 to c, 11 - 8 to b, 79 - 78 to c)\n        if (correctAnswers != answers) {\n            val incorrect = (1..4).filter { answers[it] != correctAnswers[it] }\n            Assert.fail(\"Your answers are incorrect! \$incorrect\")\n        }\n    }\n}"
    )
  }

  @ParameterizedTest
  @EnumSource(ExecutorMode::class)
  fun `koans test Generic functions`(mode: ExecutorMode) {
    runKoanTest(
      mode,
      "import java.util.*\n\nfun <T, C: MutableCollection<T>> Collection<T>.partitionTo(first: C, second: C, predicate: (T) -> Boolean): Pair<C, C> {\n    for (element in this) {\n        if (predicate(element)) {\n            first.add(element)\n        } else {\n            second.add(element)\n        }\n    }\n    return Pair(first, second)\n}\n\nfun partitionWordsAndLines() {\n    val (words, lines) = listOf(\"a\", \"a b\", \"c\", \"d e\").\n            partitionTo(ArrayList<String>(), ArrayList()) { s -> !s.contains(\" \") }\n    words == listOf(\"a\", \"c\")\n    lines == listOf(\"a b\", \"d e\")\n}\n\nfun partitionLettersAndOtherSymbols() {\n    val (letters, other) = setOf('a', '%', 'r', '}').\n            partitionTo(HashSet<Char>(), HashSet()) { c -> c in 'a'..'z' || c in 'A'..'Z'}\n    letters == setOf('a', 'r')\n    other == setOf('%', '}')\n}",
      "import org.junit.Assert\nimport org.junit.Test\nimport java.util.*\nimport koans.util.toMessageInEquals\n\nclass TestGenericFunctions {\n    @Test fun testPartitionWordsAndLines() {\n        partitionWordsAndLines()\n\n        val (words, lines) = listOf(\"a\", \"a b\", \"c\", \"d e\").\n                partitionTo(ArrayList<String>(), ArrayList()) { s -> !s.contains(\" \") }\n        Assert.assertEquals(\"partitionTo\".toMessageInEquals(), listOf(\"a\", \"c\"), words)\n        Assert.assertEquals(\"partitionTo\".toMessageInEquals(), listOf(\"a b\", \"d e\"), lines)\n    }\n\n    @Test fun testPartitionLettersAndOtherSymbols() {\n        partitionLettersAndOtherSymbols()\n\n        val (letters, other) = setOf('a', '%', 'r', '}').\n                partitionTo(HashSet<Char>(), HashSet()) { c -> c in 'a'..'z' || c in 'A'..'Z'}\n        Assert.assertEquals(\"partitionTo\".toMessageInEquals(), setOf('a', 'r'), letters)\n        Assert.assertEquals(\"partitionTo\".toMessageInEquals(), setOf('%', '}'), other)\n    }\n}"
    )
  }

}