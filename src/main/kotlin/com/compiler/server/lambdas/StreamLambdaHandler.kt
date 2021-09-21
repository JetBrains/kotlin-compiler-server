package com.compiler.server.lambdas

import com.amazonaws.serverless.exceptions.ContainerInitializationException
import com.amazonaws.serverless.proxy.model.AwsProxyRequest
import com.amazonaws.serverless.proxy.model.AwsProxyResponse
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestStreamHandler
import com.compiler.server.CompilerApplication
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class StreamLambdaHandler : RequestStreamHandler {

  companion object {
    private var handler: SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse>? = null

    init {
      try {
        handler = SpringBootLambdaContainerHandler.getAwsProxyHandler(CompilerApplication::class.java)
      } catch (e: ContainerInitializationException) {
        // if we fail here. We re-throw the exception to force another cold start
        e.printStackTrace()
        throw RuntimeException("Could not initialize Spring Boot application", e)
      }
    }
  }

  @Throws(IOException::class)
  override fun handleRequest(
    inputStream: InputStream,
    outputStream: OutputStream,
    context: Context
  ) {
    handler?.proxyStream(inputStream, outputStream, context)
  }
}
