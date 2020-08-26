package common.model

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Completion(
  val text: String,
  val displayText: String,
  val tail: String? = null,
  val import: String? = null,
  val icon: String? = null,
  var hasOtherImports: Boolean? = null
)

data class ImportInfo(
  val importName: String,
  val shortName: String,
  val fullName: String,
  val returnType: String,
  val icon: String
) {
  fun toCompletion() =
    Completion(
      text = importName,
      displayText = "$fullName  ($importName)",
      tail = returnType,
      import = importName,
      icon = icon
    )
}