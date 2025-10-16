package com.compiler.server.controllers

import com.compiler.server.model.Project
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@Deprecated(
    "Use only for old kotlin playground widget. " +
            "Should be removed in KTL-2974 around 2.5.0"
)
class KotlinPlaygroundRestController() {

    /**
     * Endpoint for support requests from kotlin playground client.
     * Kotlin Playground component see: https://github.com/JetBrains/kotlin-playground
     * Old server see: https://github.com/JetBrains/kotlin-web-demo
     */
    @RequestMapping(
        value = ["/kotlinServer"],
        method = [RequestMethod.GET, RequestMethod.POST],
        consumes = [MediaType.ALL_VALUE],
    )
    @Suppress("IMPLICIT_CAST_TO_ANY")
    fun tryKotlinLangObsoleteEndpoint(
        @RequestParam type: String,
        @RequestParam(required = false) line: Int?,
        @RequestParam(required = false) ch: Int?,
        @RequestParam(required = false) project: Project?,
        @RequestParam(defaultValue = "false") addByteCode: Boolean,
    ): ResponseEntity<String> =
        ResponseEntity
            .badRequest()
            .body(
                "This endpoint is not available anymore.\n" +
                        "And will be soon removed from the Kotlin Playground.\n" +
                        "If you are using playground widget, then update to the new version: https://github.com/JetBrains/kotlin-playground \n" +
                        "Otherwise, use the new api available at: https://api.kotlinlang.org/api/<kotlin_version>/compiler/run .\n"
            )
}
