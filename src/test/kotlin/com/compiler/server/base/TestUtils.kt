package com.compiler.server.base

import com.compiler.server.model.*
import org.junit.jupiter.api.Assertions

internal fun ExecutionResult.assertNoErrors() = errors.assertNoErrors()

internal fun Map<String, List<ErrorDescriptor>>.assertNoErrors() {
    Assertions.assertFalse(hasErrors) {
        "No errors expected, but the following errors were found:\n" +
                "\n" +
          renderErrorDescriptors(filterOnlyErrors)
    }
}

internal fun errorContains(highlights: Map<String, List<ErrorDescriptor>>, message: String) {
    Assertions.assertTrue(highlights.values.flatten().map { it.message }.any { it.contains(message) }) {
        "Haven't found diagnostic with message $message, actual diagnostics:\n" +
                "\n" +
          renderErrorDescriptors(highlights.values.flatten())
    }
    Assertions.assertTrue(highlights.values.flatten().map { it.severity }.any { it == ProjectSeveriry.ERROR })
}

internal fun warningContains(highlights: Map<String, List<ErrorDescriptor>>, message: String) {
    Assertions.assertTrue(highlights.values.flatten().map { it.message }.any { it.contains(message) }) {
        "Haven't found diagnostic with message $message, actual diagnostics:\n" +
                "\n" +
          renderErrorDescriptors(highlights.values.flatten())
    }
    Assertions.assertTrue(highlights.values.flatten().map { it.className }.any { it == "WARNING" })
    Assertions.assertTrue(highlights.values.flatten().map { it.severity }.any { it == ProjectSeveriry.WARNING })
}