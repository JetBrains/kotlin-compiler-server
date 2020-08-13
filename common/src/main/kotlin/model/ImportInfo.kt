package common.model

import common.NUMBER_OF_CHAR_IN_COMPLETION_NAME
import common.NUMBER_OF_CHAR_IN_TAIL
import common.formatName

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
    displayText = formatName("$fullName  ($importName)", NUMBER_OF_CHAR_IN_COMPLETION_NAME),
    tail = formatName(returnType, NUMBER_OF_CHAR_IN_TAIL),
    import = importName,
    icon = icon
    )
}