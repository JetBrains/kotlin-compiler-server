package completions.enums

import com.fasterxml.jackson.annotation.JsonProperty

@Suppress("UNUSED")
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
