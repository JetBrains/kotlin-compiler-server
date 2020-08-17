package common.model

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Completion(
  val text: String? = null,
  val displayText: String,
  val tail: String? = null,
  val import: String? = null,
  val icon: String? = null
)
