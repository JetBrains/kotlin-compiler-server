package completions.dto.api

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CompletionResponse(
    val text: String,
    val displayText: String,
    val tail: String? = null,
    val import: String? = null,
    val icon: Icon? = null,
    var hasOtherImports: Boolean? = null
) {

    companion object Companion {
        fun completionTextFromFullName(fullName: String): String {
            var completionText = fullName
            var position = completionText.indexOf('(')
            if (position > 0) {
                if (completionText[position - 1] == ' ') position -= 2
                if (completionText[position + 1] == ')') position++
                completionText = completionText.take(position + 1)
            }
            position = completionText.indexOf(":")
            if (position != -1) completionText = completionText.take(position - 1)
            return completionText
        }
    }
}


enum class Icon {
    @JsonProperty("class")
    CLASS,
    @JsonProperty("method")
    METHOD,
    @JsonProperty("property")
    PROPERTY,
    @JsonProperty("package")
    PACKAGE,
    @JsonProperty("genericValue")
    GENERIC_VALUE
}
