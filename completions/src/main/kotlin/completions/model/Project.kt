package completions.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonValue

@JsonIgnoreProperties(ignoreUnknown = true)
data class Project(
    val args: String = "",
    val files: List<ProjectFile> = listOf(),
    val confType: ProjectType = ProjectType.JAVA
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ProjectFile(val text: String = "", val name: String = "")

@Suppress("unused")
enum class ProjectType(@JsonValue val id: String) {
    JAVA("java"),
    JUNIT("junit"),
    JS("js"),
    CANVAS("canvas"),
    JS_IR("js-ir"),
    WASM("wasm"),
    COMPOSE_WASM("compose-wasm");

}