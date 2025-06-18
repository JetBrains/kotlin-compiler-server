package com.compiler.server.base

import java.io.IOException


@Throws(IOException::class, InterruptedException::class)
fun startNodeJsApp(
    pathToBinNode: String?,
    pathToAppScript: String?
): String {
    val processBuilder = ProcessBuilder()
    processBuilder.command(pathToBinNode, pathToAppScript)
    val process = processBuilder.start()
    val inputStream = process.inputStream
    process.waitFor()
    return inputStream.reader().readText()
}