package model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Completion(
  val text: String,
  val displayText: String,
  val tail: String? = null,
  val import: String? = null,
  val icon: Icon? = null,
  var hasOtherImports: Boolean? = null
)

@JsonIgnoreProperties(value = ["shortName"])
data class ImportInfo(
  val importName: String,
  val shortName: String? = null,
  val fullName: String,
  val returnType: String,
  val icon: Icon
) {
  fun toCompletion() =
    Completion(
      text = "${importName.substringBeforeLast('.')}.${completionTextFromFullName(fullName)}",
      displayText = "$fullName  ($importName)",
      tail = returnType,
      import = importName,
      icon = icon
    )
}

fun completionTextFromFullName(fullName: String): String {
  var completionText = fullName
  var position = completionText.indexOf('(')
  if (position > 0) {
    if (completionText[position - 1] == ' ') position -= 2
    if (completionText[position + 1] == ')') position++
    completionText = completionText.substring(0, position + 1)
  }
  position = completionText.indexOf(":")
  if (position != -1) completionText = completionText.substring(0, position - 1)
  return completionText
}