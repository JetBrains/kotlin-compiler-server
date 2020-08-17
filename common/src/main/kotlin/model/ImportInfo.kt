package common.model

data class ImportInfo(
  val importName: String,
  val shortName: String,
  val fullName: String,
  val returnType: String,
  val icon: String
) {
  fun toCompletion() =
    Completion(
      displayText = "$fullName  ($importName)",
      tail = returnType,
      import = importName,
      icon = icon
    )
}