package com.compiler.server.base

import java.io.IOException


@Throws(IOException::class, InterruptedException::class)
fun startNodeJsApp(
    pathToBinNode: String?,
    pathToAppScript: String?,
    additionalScript: String? = null
): String {
    val processBuilder = ProcessBuilder()
    if (additionalScript != null)
        processBuilder.command(pathToBinNode, "--import", additionalScript, pathToAppScript)
    else
        processBuilder.command(pathToBinNode, pathToAppScript)
    val process = processBuilder.start()
    val inputStream = process.inputStream
    process.waitFor()
    return inputStream.reader().readText()
}