# Kotlin compiler server

[![official JetBrains project](https://jb.gg/badges/official-plastic.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
![Build status](https://buildserver.labs.intellij.net/app/rest/builds/buildType:id:Kotlin_KotlinSites_Deployments_PlayKotlinlangOrg_Backend_BuildMaster/statusIcon.svg)
![Java CI](https://github.com/JetBrains/kotlin-compiler-server/workflows/Java%20CI/badge.svg)
![TC status](https://img.shields.io/teamcity/build/s/Kotlin_KotlinPlayground_KotlinCompilerServer_Build?label=TeamCity%20build)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.7.20-orange.svg) ](https://kotlinlang.org/)
[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](https://www.apache.org/licenses/LICENSE-2.0)

A REST server for compiling and executing Kotlin code.
The server provides the API for [Kotlin Playground](https://github.com/JetBrains/kotlin-playground) library.

## How to start :checkered_flag:

### Simple Spring Boot application

Download Kotlin dependencies and build an executor before starting the server:

```shell script
$ ./gradlew build -x test 
```

Start the Spring Boot project. The main class: `com.compiler.server.CompilerApplication`

### With Docker

To build the app inside a Docker container, run the following command from the project directory:
```shell
$ ./docker-image-build.sh
```

### From Amazon lambda

Based on [aws-serverless-container](https://github.com/awslabs/aws-serverless-java-container).

```shell script
$ ./gradlew buildLambda
```

Getting `.zip` file from `build/distributions`.

Lambda handler: `com.compiler.server.lambdas.StreamLambdaHandler::handleRequest`.

Publish your Lambda function: you can follow the instructions
in [AWS Lambda's documentation](https://docs.aws.amazon.com/lambda/latest/dg/lambda-java-how-to-create-deployment-package.html)
on how to package your function for deployment.

### From Kotless

Add [Kotless](https://github.com/JetBrains/kotless) and
remove [aws-serverless-container](https://github.com/awslabs/aws-serverless-java-container) =)

## API Documentation :page_with_curl:

Swagger url: http://localhost:8080/swagger-ui/

## How to add your dependencies to kotlin compiler :books:

Just put whatever you need as dependencies
to [build.gradle.kts](https://github.com/JetBrains/kotlin-compiler-server/blob/master/build.gradle.kts) via a
task called `kotlinDependency`:

```
 kotlinDependency "your dependency"
```

NOTE: If the library you're adding uses reflection, accesses the file system, or performs any other type of
security-sensitive operations, don't forget to
configure
the [executor.policy](https://github.com/JetBrains/kotlin-compiler-server/blob/master/executor.policy)
. [Click here](https://docs.oracle.com/javase/7/docs/technotes/guides/security/PolicyFiles.html) for more information
about *Java Security Policy*.

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

| ENV                               | Default value |
|-----------------------------------|---------------|
| ACCESS_CONTROL_ALLOW_ORIGIN_VALUE | *             |
| ACCESS_CONTROL_ALLOW_HEADER_VALUE | *             |

## Configure logging

We use `prod` spring active profile to stream logs as JSON format.
You can set the spring profile by supplying `-Dspring.profiles.active=prod` or set env variable `SPRING_PROFILES_ACTIVE` to `prod` value.

### Unsuccessful execution logs

In case of an unsuccessful execution in the standard output will be the event with INFO level:

```json
{
  "date_time": "31/Aug/2021:11:49:45 +03:00",
  "@version": "1",
  "message": "Code execution is complete.",
  "logger_name": "com.compiler.server.service.KotlinProjectExecutor",
  "thread_name": "http-nio-8080-exec-1",
  "level": "INFO",
  "level_value": 20000,
  "hasErrors": true,
  "confType": "JAVA",
  "kotlinVersion": "$kotlinVersion"
}
```

## Kotlin release guide :rocket:

1) Update the kotlin version
   in [libs.versions.toml](https://github.com/JetBrains/kotlin-compiler-server/blob/master/gradle/libs.versions.toml)
2) Make sure everything is going well via the task:

```shell script
$ ./gradlew build
```

3) Save branch with the name of the kotlin version. Pattern: `/^[0-9.]+$/`  (optional)
4) Bump version on GitHub [releases](https://github.com/JetBrains/kotlin-compiler-server/releases) (optional)
