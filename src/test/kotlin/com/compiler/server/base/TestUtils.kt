package com.compiler.server.base

import com.compiler.server.model.ErrorDescriptor
import com.compiler.server.model.ExecutionResult
import com.compiler.server.model.ProjectSeveriry
import org.junit.jupiter.api.Assertions

internal fun ExecutionResult.assertNoErrors() = compilerDiagnostics.assertNoErrors()

internal fun List<ErrorDescriptor>.assertNoErrors() {
    Assertions.assertFalse(hasErrors) {
        "No errors expected, but the following errors were found:\n" +
                "\n" +
          renderErrorDescriptors(filterOnlyErrors)
    }
}

internal fun errorContains(highlights: List<ErrorDescriptor>, message: String) {
    Assertions.assertTrue(highlights.any { it.message.contains(message) }) {
        "Haven't found diagnostic with message $message, actual diagnostics:\n" +
                "\n" +
          renderErrorDescriptors(highlights)
    }
    Assertions.assertTrue(highlights.any { it.severity == ProjectSeveriry.ERROR }) { highlights.toString() }
}

internal fun warningContains(highlights: List<ErrorDescriptor>, message: String) {
    Assertions.assertTrue(highlights.any { it.message.contains(message) }) {
        "Haven't found diagnostic with message $message, actual diagnostics:\n" +
                "\n" +
          renderErrorDescriptors(highlights)
    }
    Assertions.assertTrue(highlights.any { it.className == "WARNING" }) { highlights.toString() }
    Assertions.assertTrue(highlights.any { it.severity == ProjectSeveriry.WARNING }) { highlights.toString() }
}
