package com.compiler.server.controllers

import com.compiler.server.model.bean.VersionInfo
import com.compiler.server.service.KotlinProjectExecutor
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/versions")
class VersionRestController(private val kotlinProjectExecutor: KotlinProjectExecutor) {
    @GetMapping
    fun getKotlinVersionEndpoint(): List<VersionInfo> = listOf(kotlinProjectExecutor.getVersion())
}