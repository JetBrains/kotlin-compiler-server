package com.compiler.server.base

import com.compiler.server.model.ErrorDescriptor
import com.compiler.server.model.ExecutionResult
import com.compiler.server.model.ProjectSeveriry
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
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
    assertThat(
        highlights.map { it.message }, hasItem(
            containsString(message)
        )
    )

    assertThat(
        highlights.map { it.severity }, hasItem(
            equalTo(ProjectSeveriry.ERROR)
        )
    )
}

internal fun warningContains(highlights: List<ErrorDescriptor>, message: String) {
    assertThat(
        highlights.map { it.message }, hasItem(
            containsString(message)
        )
    )

    assertThat(
        highlights.map { it.className }, hasItem(
            "WARNING"
        )
    )

    assertThat(
        highlights.map { it.severity }, hasItem(
            equalTo(ProjectSeveriry.WARNING)
        )
    )
}
