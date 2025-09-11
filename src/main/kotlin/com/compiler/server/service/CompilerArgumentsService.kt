package com.compiler.server.service

import com.compiler.server.model.ExtendedCompilerArgument
import com.compiler.server.model.ProjectType
import org.jetbrains.kotlin.utils.filterToSetOrEmpty
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.concurrent.ConcurrentHashMap

@Service
class CompilerArgumentsService(
    private val jvmCompilerArguments: Set<ExtendedCompilerArgument>,
    private val wasmCompilerArguments: Set<ExtendedCompilerArgument>,
    private val composeWasmCompilerArguments: Set<ExtendedCompilerArgument>,
    private val jsCompilerArguments: Set<ExtendedCompilerArgument>,
) {
    private val cache = ConcurrentHashMap<ProjectType, Set<ExtendedCompilerArgument>>()

    fun getCompilerArguments(projectType: ProjectType): Set<ExtendedCompilerArgument> {
        return cache.computeIfAbsent(projectType) {
            when (it) {
                ProjectType.JAVA, ProjectType.JUNIT -> jvmCompilerArguments
                ProjectType.WASM -> wasmCompilerArguments
                ProjectType.COMPOSE_WASM -> composeWasmCompilerArguments
                ProjectType.JS_IR -> jsCompilerArguments
                else -> throw ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unsupported projectType '$projectType' for compiler arguments discovery"
                )
            }.filterToSetOrEmpty { arg -> arg.supportedOnCurrentVersion }
        }
    }
}