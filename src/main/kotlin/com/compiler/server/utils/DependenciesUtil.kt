package com.compiler.server.utils

import com.compiler.server.common.components.KotlinEnvironment
import com.compiler.server.common.components.PATH_SEPARATOR
import com.compiler.server.model.*
import com.compiler.server.model.bean.VersionInfo
import org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgument
import org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArguments
import org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgumentsLevel
import org.jetbrains.kotlin.arguments.dsl.types.*
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.springframework.stereotype.Component

@Component
class DependenciesUtil(
    kotlinEnvironment: KotlinEnvironment,
) {

    val dependenciesComposeWasm: String = kotlinEnvironment.dependenciesComposeWasm
}