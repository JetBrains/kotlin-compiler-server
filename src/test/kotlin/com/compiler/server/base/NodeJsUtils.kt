package com.compiler.server.base

import java.io.IOException
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi


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

private val BASE64_DATA_URI_REGEX =
    Regex("""data:application/javascript;base64,\s*([A-Za-z0-9+/=]+)""")

@OptIn(ExperimentalEncodingApi::class)
fun replaceStaticUrlInJs(jsContent: String, staticUrl: String, replacement: String): String {
    return BASE64_DATA_URI_REGEX.replace(jsContent) { match ->
        val encoded = match.groupValues[1]
        val decoded = Base64.decode(encoded).toString(Charsets.UTF_8)
        if (staticUrl in decoded) {
            val replaced = decoded.replace(staticUrl, replacement)
            "data:application/javascript;base64,${Base64.encode(replaced.toByteArray())}"
        } else {
            match.value
        }
    }
}
