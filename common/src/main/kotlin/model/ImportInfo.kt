package common.model

import common.formatCompletionName
import common.formatTailName

data class ImportInfo(
  val importName: String,
  val shortName: String,
  val fullName: String,
  val returnType: String,
  val icon: String
) {
  fun toCompletion() =
    Completion(
    text = "$fullName  ($importName)",
    displayText = formatCompletionName("$fullName  ($importName)"),
    tail = formatTailName(returnType),
    import = importName,
    icon = icon
    )
}