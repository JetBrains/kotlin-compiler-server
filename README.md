# Kotlin compiler server [![Build Status](https://travis-ci.com/AlexanderPrendota/kotlin-compiler-server.svg?branch=master)](https://travis-ci.com/AlexanderPrendota/kotlin-compiler-server) [ ![Kotlin](https://img.shields.io/badge/Kotlin-1.3.60-orange.svg) ](https://kotlinlang.org/) [![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](https://www.apache.org/licenses/LICENSE-2.0)

REST API for compile Kotlin code.
Server provides API for [Kotlin Playground](https://github.com/JetBrains/kotlin-playground) library.

## How to start

Download Kotlin dependencies and build executor before starting the server:

```shell script
$ ./gradlew build -x test 
```

Start Spring Boot project.

## API Documentation

**Run code JVM**

```shell script
curl -X POST \
  http://localhost:8080/api/compiler/run \
  -H 'Content-Type: application/json' \
  -d '{
    "args": "1 2 3",
    "files": [
        {
            "name": "File.kt",
            "text": "fun main(args: Array<String>) {\n    println(\"123\")\n}"
        }
    ]
}'
```

**Translate Kotlin code to JavaScript code**

```shell script
curl -X POST \
    http://localhost:8080/api/compiler/translate \
    -H 'Content-Type: application/json' \
    -d '{
      "args": "1 2 3",
      "files": [
        {
          "name": "File.kt",
          "text": "fun main(args: Array<String>) {\n    println(args[0])\n }"
        }
      ]
}'
```

**Run tests**

```shell script
curl -X POST \
  http://localhost:8080/api/compiler/test \
  -H 'Content-Type: application/json' \
  -d '{
  "files": [
    {
      "name": "File.kt",
      "text": "fun start(): String = \"OK\""
    },
    {
      "name": "test0.kt",
      "text": "import org.junit.Assert\nimport org.junit.Test\n\nclass TestStart {\n    @Test fun testOk() {\n        Assert.assertEquals(\"OK\", start())\n    }\n}"
    },
    {
      "name": "test1.kt",
      "text": "package koans.util\n\nfun String.toMessage() = \"The function '\''$this'\'' is implemented incorrectly\"\n\nfun String.toMessageInEquals() = toMessage().inEquals()\n\nfun String.inEquals() = this"
    }
  ]
}'
```

**Code completions**

```shell script
curl -X POST \
  'http://localhost:8080/api/compiler/complete?line=1&ch=12' \
  -H 'Content-Type: application/json' \
  -d '{
  "files": [
    {
      "name": "File.kt",
      "text": "fun main() {\n    3.0.toIn\n}"
    }
  ]
}'
```

**Code analytics**

```shell script
curl -X POST \
  http://localhost:8080/api/compiler/highlight \
  -H 'Content-Type: application/json' \
  -d '{
  "files": [
    {
      "name": "File.kt",
      "text": "fun main() {\n    println(\"Hello, world!!!\")ass\n}"
    }
  ]
}'
```

**Get Kotlin version**

```shell script
curl -X GET http://localhost:8080/api/compiler/version
```


Also server supports [API](https://github.com/JetBrains/kotlin-playground) for Kotlin Playground library. 

## How to add your dependencies to kotlin compiler :books:

Just put whatever you need as dependencies to [build.gradle.kts](https://github.com/AlexanderPrendota/kotlin-compiler-server/blob/master/build.gradle.kts) via a task called `kotlinDependency`:

```
 kotlinDependency "your dependency"
```

NOTE: If the library you're adding uses reflection, accesses the file system, or performs any other type of security-sensitive operations, don't forget to
configure the [executors.policy](https://github.com/AlexanderPrednota/kotlin-compiler-server/blob/master/executors.policy). [Click here](https://docs.oracle.com/javase/7/docs/technotes/guides/security/PolicyFiles.html) for more information about *Java Security Policy*.

**How to set Java Security Policy in `executors.policy`**

If you want to configure a custom dependency, use the marker `@LIB_DIR@`:

```
grant codeBase "file:%%LIB_DIR%%/junit-4.12.jar"{
  permission java.lang.reflect.ReflectPermission "suppressAccessChecks";
  permission java.lang.RuntimePermission "setIO";
  permission java.io.FilePermission "<<ALL FILES>>", "read";
  permission java.lang.RuntimePermission "accessDeclaredMembers";
};
```

## Documentation

TODO: Describe API

Tasks:

1) Tests on highlight + warnings (+)
2) Tests on complete (+)
3) Support old API (+)
4) Add executor policy (+)
5) Add dockerfile (?)
6) Test multi-version with spring starters (-)
7) New dir structure (+)
8) Support JUNIT (+)
9) Documentation (+)
10) Test for compiler errors (+)
12) Endpoint for versions (+)
13) Readonly files (+)
14) move `lib` to  config props (+)
15) add coroutines (+)
16) add coroutines test (+)
17) test junit exception in executor (+)
18) js env from ErrAnalyzer to EnvManager (+)
19) Get kotlin plugin form marketplace (+)
20) Bug with exception in executor. See coroutines test 13 (+)
21) Fix InterruptExecutionTest (+)
22) Tests on exception (+)
23) Validate compiler exceptions (+)
24) support old kotlin version via config + redirect on try.kotl.in
25) add coroutines tests