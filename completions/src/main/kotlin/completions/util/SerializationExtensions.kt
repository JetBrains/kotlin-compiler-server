package completions.util

import com.fasterxml.jackson.databind.JsonNode

object SerializationExtensions {
    fun JsonNode.walk(fieldName: String): JsonNode? = path(fieldName).takeIf { !it.isMissingNode }
}