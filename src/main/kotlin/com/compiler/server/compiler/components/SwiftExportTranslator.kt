package com.compiler.server.compiler.components

import com.compiler.server.model.CompilerDiagnostics
import com.compiler.server.model.SwiftExportResult
import com.compiler.server.model.toExceptionDescriptor
import component.KotlinEnvironment
import org.jetbrains.kotlin.psi.KtFile
import org.springframework.stereotype.Component
import runSwiftExport
import java.nio.file.Path

@Component
class SwiftExportTranslator(
    private val kotlinEnvironment: KotlinEnvironment,
) {
    fun translate(files: List<KtFile>): SwiftExportResult = try {
        usingTempDirectory { tempDirectory ->
            val ioFiles = files.writeToIoFiles(tempDirectory)
            val stdlib = kotlinEnvironment.WASM_LIBRARIES.singleOrNull { "stdlib" in it }
            val swiftCode = runSwiftExport(
                sourceFile = ioFiles.first(),
                stdlibPath = stdlib?.let { Path.of(it) },
            )
            SwiftExportResult(
                compilerDiagnostics = CompilerDiagnostics(emptyMap()),
                swiftCode = swiftCode
            )
        }
    } catch (e: Exception) {
        SwiftExportResult(swiftCode = "", exception = e.toExceptionDescriptor())
    }
}