package common.model

data class Completion(
  val text: String,
  val displayText: String,
  val tail: String? = null,
  val import: String? = null,
  val icon: String? = null
)
