# Kotlin compiler server
[![Build Status](https://travis-ci.com/AlexanderPrendota/kotlin-compiler-server.svg?branch=master)](https://travis-ci.com/AlexanderPrendota/kotlin-compiler-server)
![Java CI](https://github.com/AlexanderPrendota/kotlin-compiler-server/workflows/Java%20CI/badge.svg)
![TC status](https://img.shields.io/teamcity/build/s/Kotlin_KotlinPlayground_KotlinCompilerServer_Build?label=TeamCity%20build) 
[![Kotlin](https://img.shields.io/badge/Kotlin-1.4.30-orange.svg) ](https://kotlinlang.org/) 
[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](https://www.apache.org/licenses/LICENSE-2.0)
![Docker Cloud Build Status](https://img.shields.io/docker/cloud/build/prendota/kotlin-compiler-server)

A REST server for compiling and executing Kotlin code.
The server provides the API for [Kotlin Playground](https://github.com/JetBrains/kotlin-playground) library.

## How to start :checkered_flag:

### Simple Spring Boot application

Download Kotlin dependencies and build an executor before starting the server:

```shell script
$ ./gradlew build -x test 
```

Start the Spring Boot project. The main class: `com.compiler.server.CompilerApplication`

### From Docker Hub

View images on [Docker Hub](https://hub.docker.com/r/prendota/kotlin-compiler-server).

```docker
docker pull prendota/kotlin-compiler-server
```

### From Amazon lambda

Based on [aws-serverless-container](https://github.com/awslabs/aws-serverless-java-container).

```shell script
$ ./gradlew buildLambda
```

Getting `.zip` file from `build/distributions`.

Lambda handler: `com.compiler.server.lambdas.StreamLambdaHandler::handleRequest`.

Publish your Lambda function: you can follow the instructions in [AWS Lambda's documentation](https://docs.aws.amazon.com/lambda/latest/dg/lambda-java-how-to-create-deployment-package.html) on how to package your function for deployment.

### From Kotless

Add [Kotless](https://github.com/JetBrains/kotless) and remove [aws-serverless-container](https://github.com/awslabs/aws-serverless-java-container) =)

## API Documentation :page_with_curl:

### Execute Kotlin code on JVM

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

### Translate Kotlin code to JavaScript code

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

### Run Kotlin tests

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

### Get code completions for a specified place in code 

```shell script
curl -X POST \
  'http://localhost:8080/api/compiler/complete?line=2&ch=15' \
  -H 'Content-Type: application/json' \
  -d '{
  "files": [
    {
      "name": "File.kt",
      "text": "fun main() {\n    val sinusoid = "sinusoid"\n    val s = sin\n}"
    }
  ]
}'
```

### Get code analysis results

```shell script
curl -X POST \
  http://localhost:8080/api/compiler/highlight \
  -H 'Content-Type: application/json' \
  -d '{
  "files": [
    {
      "name": "File.kt",
      "text": "fun main() {\n    println(\"Hello, world!!!\")ass\n    val random = Random\n}"
    }
  ]
}'
```

### Get the current Kotlin version

```shell script
curl -X GET http://localhost:8080/versions
```


The server also supports an [API](https://github.com/JetBrains/kotlin-playground) for the Kotlin Playground library. 

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

## CORS configuration

Set the environment variables

| ENV                | Default value        |
| -------------------|----------------------|
| ACCESS_CONTROL_ALLOW_ORIGIN_VALUE| *|
| ACCESS_CONTROL_ALLOW_HEADER_VALUE| *|

## Kotlin release guide :rocket:

1) Update the kotlin version in [gradle.properties](https://github.com/AlexanderPrendota/kotlin-compiler-server/blob/master/gradle.properties)
2) Update the kotlin version in [build.gradle.kts](https://github.com/AlexanderPrendota/kotlin-compiler-server/blob/1a12996f40a5d3391bc06d2ddd719cbfe2578802/build.gradle.kts#L29) 
3) Update the kotlin version in [Dockerfile](https://github.com/AlexanderPrendota/kotlin-compiler-server/blob/master/Dockerfile)
4) Make sure everything is going well via the task: 

```shell script
$ ./gradlew build
```

5) Save branch with the name of the kotlin version. Pattern: `/^[0-9.]+$/`  (optional)
6) Bump version on GitHub [releases](https://github.com/AlexanderPrendota/kotlin-compiler-server/releases) (optional)
