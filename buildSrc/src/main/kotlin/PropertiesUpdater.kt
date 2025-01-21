import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

abstract class ComposeWasmPropertiesUpdater : DefaultTask() {

    @get:Input
    abstract val propertiesPath: Property<String>

    @get:InputFile
    abstract val typeInfoFile: RegularFileProperty

    @get:Input
    abstract val propertiesMap: MapProperty<String, String>

    @get:OutputFile
    val updatedPropertiesFile: RegularFileProperty = project.objects.fileProperty().fileProvider(propertiesPath.map { File(it) })

    @TaskAction
    fun updateProperties() {
        val file = File(propertiesPath.get())

        propertiesMap.get().let {
            if (it.isNotEmpty()) {
                file.writeText("")
                it.forEach { (key, value) ->
                    file.appendText("$key=$value\n")
                }
            }
        }

        file.appendText(
            "\ndependencies.compose.wasm=${hashFileContent(typeInfoFile.get().asFile.absolutePath)}"
        )
    }
}

fun hashFileContent(filePath: String, hashAlgorithm: String = "SHA-256"): String {
    val file = File(filePath)
    val digest = MessageDigest.getInstance(hashAlgorithm)

    // Read the file content in chunks and update the digest
    FileInputStream(file).use { fileInputStream ->
        val buffer = ByteArray(1024)
        var bytesRead: Int
        while (fileInputStream.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }
    }

    // Convert the resulting byte array to a readable hex string
    return digest.digest().joinToString("") { "%02x".format(it) }
}