package com.compiler.server.utils

fun escapeString(string: String): String? {
  var resultString = string
  if (resultString.isNotEmpty()) {
    if (resultString.contains("<")) {
      resultString = resultString.replace("<".toRegex(), "&lt;")
    }
    if (resultString.contains(">")) {
      resultString = resultString.replace(">".toRegex(), "&gt;")
    }
    if (resultString.contains("&")) {
      resultString = resultString.replace("&".toRegex(), "&amp;")
    }
  }
  return resultString
}