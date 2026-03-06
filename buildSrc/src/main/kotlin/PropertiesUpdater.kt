import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

abstract class PropertiesGenerator : DefaultTask() {

    @get:Input
    abstract val propertiesMap: MapProperty<String, String>

    @get:OutputFile
    abstract val propertiesFile: RegularFileProperty

    @TaskAction
    fun updateProperties() {
        val file = propertiesFile.get().asFile

        propertiesMap.get().let {
            if (it.isNotEmpty()) {
                file.writeText("")
                it.forEach { (key, value) ->
                    file.appendText("$key=$value\n")
                }
            }
        }
    }
}

fun hashFileContent(files: File, hashAlgorithm: String = "SHA-256"): String {
    val digest = MessageDigest.getInstance(hashAlgorithm)

    files.listFiles()
        .filter { it.isFile }
        .forEach { file ->
            // Read the file content in chunks and update the digest
            FileInputStream(file).use { fileInputStream ->
                val buffer = ByteArray(1024)
                var bytesRead: Int
                while (fileInputStream.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
        }

    // Convert the resulting byte array to a readable hex string
    return digest.digest().joinToString("") { "%02x".format(it) }
}