package com.compiler.server.utils

import com.compiler.server.common.components.KotlinEnvironment
import com.compiler.server.common.components.PATH_SEPARATOR
import com.compiler.server.model.BooleanExtendedCompilerArgumentValue
import com.compiler.server.model.ExtendedCompilerArgument
import com.compiler.server.model.ExtendedCompilerArgumentValue
import com.compiler.server.model.ListExtendedCompilerArgumentValue
import com.compiler.server.model.StringExtendedCompilerArgumentValue
import com.compiler.server.model.bean.VersionInfo
import org.jetbrains.kotlin.arguments.description.CompilerArgumentsLevelNames.jsArguments
import org.jetbrains.kotlin.arguments.description.CompilerArgumentsLevelNames.jvmCompilerArguments
import org.jetbrains.kotlin.arguments.description.CompilerArgumentsLevelNames.wasmArguments
import org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgument
import org.jetbrains.kotlin.arguments.dsl.types.BooleanType
import org.jetbrains.kotlin.arguments.dsl.types.IntType
import org.jetbrains.kotlin.arguments.dsl.types.KlibIrInlinerModeType
import org.jetbrains.kotlin.arguments.dsl.types.KotlinArgumentValueType
import org.jetbrains.kotlin.arguments.dsl.types.KotlinExplicitApiModeType
import org.jetbrains.kotlin.arguments.dsl.types.KotlinHeaderModeType
import org.jetbrains.kotlin.arguments.dsl.types.KotlinJvmTargetType
import org.jetbrains.kotlin.arguments.dsl.types.KotlinVersionType
import org.jetbrains.kotlin.arguments.dsl.types.ReturnValueCheckerModeType
import org.jetbrains.kotlin.arguments.dsl.types.StringArrayType
import org.jetbrains.kotlin.arguments.dsl.types.StringType
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.springframework.stereotype.Component

internal const val JS_DEFAULT_MODULE_NAME = "playground"
internal const val WASM_DEFAULT_MODULE_NAME = "playground"
internal const val JS_BUILTINS_POSTFIX = "js-builtins"
internal const val IMPORT_OBJECT_POSTFIX = "import-object"

@Component
class CompilerArgumentsUtil(
    private val versionInfo: VersionInfo,
    kotlinEnvironment: KotlinEnvironment
) {

    internal val ALLOWED_COMMON_TOOL_ARGUMENTS = setOf(
        "nowarn",
        "Werror",
        "Wextra"
    )

    internal val ALLOWED_COMMON_ARGUMENTS = setOf(
        "XXLanguage",
        "Xexplicit-backing-fields",
        "progressive",
        "opt-in",
        "Xno-inline",
        "Xskip-metadata-version-check",
        "Xskip-prerelease-check",
        "Xallow-kotlin-package",
        "Xstdlib-compilation",
        "Xno-check-actual",
        "Xnew-inference",
        "Xinline-classes",
        "Xverify-ir",
        "Xverify-ir-visibility",
        "Xcheck-phase-conditions",
        "Xuse-k2",
        "Xuse-fir-experimental-checkers",
        "Xuse-fir-ic",
        "Xuse-fir-lt",
        "Xdisable-default-scripting-plugin",
        "Xexplicit-api",
        "XXexplicit-return-types",
        "Xreturn-value-checker",
        "Xsuppress-version-warnings",
        "Xsuppress-api-version-greater-than-language-version-error",
        "Xexpect-actual-classes",
        "Xconsistent-data-class-copy-visibility",
        "Xunrestricted-builder-inference",
        "Xcontext-receivers",
        "Xcontext-parameters",
        "Xcontext-sensitive-resolution",
        "Xnon-local-break-continue",
        "Xdata-flow-based-exhaustiveness",
        "Xdirect-java-actualization",
        "Xmulti-dollar-interpolation",
        "Xenable-incremental-compilation",
        "Xrender-internal-diagnostic-names",
        "Xreport-all-warnings",
        "Xignore-const-optimization-errors",
        "Xdont-warn-on-error-suppression",
        "Xwhen-guards",
        "Xnested-type-aliases",
        "Xsuppress-warning",
        "Xwarning-level",
        "Xannotation-default-target",
        "Xannotation-target-all",
        "XXlenient-mode",
        "Xallow-reified-type-in-catch",
        "Xallow-contracts-on-more-functions",
        "Xallow-condition-implies-returns-contracts",
        "Xallow-holdsin-contract"
    )

    internal val ALLOWED_JVM_ARGUMENTS = ALLOWED_COMMON_TOOL_ARGUMENTS + ALLOWED_COMMON_ARGUMENTS + setOf(
        // file paths and environment settings
        "include-runtime",
        "no-jdk",
        "no-stdlib",
        "no-reflect",
        "module-name",
        "jvm-target",
        "java-parameters",
        "jvm-default",
        "Xallow-unstable-dependencies",
        "Xabi-stability",
        "Xir-do-not-clear-binding-context",
        "Xno-call-assertions",
        "Xno-receiver-assertions",
        "Xno-param-assertions",
        "Xno-optimize",
        "Xassertions",
        "Xuse-type-table",
        "Xuse-fast-jar-file-system",
        "Xsuppress-missing-builtins-error",
        "Xjsr305",
        "Xnullability-annotations",
        "Xsupport-compatqual-checker-framework-annotations",
        "Xjspecify-annotations",
        "Xjvm-default",
        "Xgenerate-strict-metadata-version",
        "Xsanitize-parentheses",
        "Xemit-jvm-type-annotations",
        "Xjvm-expose-boxed",
        "Xstring-concat",
        "Xsam-conversions",
        "Xlambdas",
        "Xindy-allow-annotated-lambdas",
        "Xno-reset-jar-timestamps",
        "Xno-unified-null-checks",
        "Xno-source-debug-extension",
        "Xjvm-enable-preview",
        "Xsuppress-deprecated-jvm-target-warning",
        "Xserialize-ir",
        "Xtype-enhancement-improvements-strict-mode",
        "Xvalidate-bytecode",
        "Xenhance-type-parameter-types-to-def-not-null",
        "Xlink-via-signatures",
        "Xno-new-java-annotation-targets",
        "Xvalue-classes",
        "Xir-inliner",
        "Xuse-inline-scopes-numbers",
        "Xuse-k2-kapt",
        "Xcompile-builtins-as-part-of-stdlib",
        "Xannotations-in-metadata",
        "Xwhen-expressions",
        "Xheader-mode-type"
    )

    internal val ALLOWED_COMMON_KLIB_BASED_ARGUMENTS = setOf(
        "Xklib-enable-signature-clash-checks",
        "Xpartial-linkage",
        "Xpartial-linkage-loglevel",
        "Xklib-duplicated-unique-name-strategy",
        "Xklib-ir-inliner"
    )

    internal val ALLOWED_JS_ARGUMENTS =
        ALLOWED_COMMON_TOOL_ARGUMENTS + ALLOWED_COMMON_ARGUMENTS + ALLOWED_COMMON_KLIB_BASED_ARGUMENTS + setOf(
            // file paths and environment settings
            "Xir-keep",
            "main",
            "Xir-dce",
            "Xir-dce-runtime-diagnostic",
            "Xir-property-lazy-initialization",
            "Xir-minimized-member-names",
            "Xir-generate-inline-anonymous-functions",
            "Xgenerate-polyfills",
            "Xes-classes",
            "Xplatform-arguments-in-main-function",
            "Xes-generators",
            "Xes-arrow-functions",
            "Xes-long-as-bigint",
            "Xtyped-arrays",
            "Xenable-extension-functions-in-externals",
            "Xenable-suspend-function-exporting",
            "Xfake-override-validator",
            "Xoptimize-generated-js",
        )

    internal val ALLOWED_WASM_ARGUMENTS =
        ALLOWED_COMMON_TOOL_ARGUMENTS + ALLOWED_COMMON_ARGUMENTS + ALLOWED_COMMON_KLIB_BASED_ARGUMENTS + setOf(
            // file paths and environment settings
            "Xwasm-debug-info",
            "Xwasm-debug-friendly",
            "Xwasm-kclass-fqn",
            "Xwasm-enable-array-range-checks",
            "Xwasm-enable-asserts",
            "Xwasm-use-traps-instead-of-exceptions",
            "Xwasm-use-new-exception-proposal",
            "Xwasm-no-jstag",
            "Xwasm-source-map-include-mappings-from-unavailable-sources"
        )

    // Use Pair if you want to provide different values for user and for actual use.
    // For example, with Xplugin users need to see only plugins, without an actual path,
    // but for the compiler we need to pass a full path to the plugin jar.
    // In declared Pair the first element is for the user, the second is for actual use.
    val PREDEFINED_JVM_ARGUMENTS = mapOf(
        "classpath" to Pair(
            kotlinEnvironment.classpath.joinToString(PATH_SEPARATOR) { it.name },
            kotlinEnvironment.classpath.joinToString(PATH_SEPARATOR) { it.absolutePath }),
        "module-name" to "web-module",
        "no-stdlib" to true,
        "no-reflect" to true,
        "progressive" to true,
        "Xplugin" to Pair(
            kotlinEnvironment.compilerPlugins.map { it.name },
            kotlinEnvironment.compilerPlugins.map { it.absolutePath }),
        "opt-in" to listOf(
            "kotlin.ExperimentalStdlibApi",
            "kotlin.time.ExperimentalTime",
            "kotlin.RequiresOptIn",
            "kotlin.ExperimentalUnsignedTypes",
            "kotlin.contracts.ExperimentalContracts",
            "kotlin.experimental.ExperimentalTypeInference",
            "kotlin.uuid.ExperimentalUuidApi",
            "kotlin.io.encoding.ExperimentalEncodingApi",
            "kotlin.concurrent.atomics.ExperimentalAtomicApi"
        ),
        "Xcontext-parameters" to true,
        "Xnested-type-aliases" to true,
        "Xreport-all-warnings" to true,
        "Wextra" to true,
        "Xexplicit-backing-fields" to true,
        "XXLanguage" to "+ExplicitBackingFields"
    )

    val PREDEFINED_WASM_FIRST_PHASE_ARGUMENTS = mapOf(
        "Xreport-all-warnings" to true,
        "Wextra" to true,
        "Xwasm" to true,
        "Xir-produce-klib-dir" to true,
        "libraries" to Pair(
            kotlinEnvironment.WASM_LIBRARIES.sorted().joinToString(PATH_SEPARATOR) { it.split("/").last() },
            kotlinEnvironment.WASM_LIBRARIES.joinToString(PATH_SEPARATOR)
        ),
        "ir-output-name" to WASM_DEFAULT_MODULE_NAME,
    )

    val PREDEFINED_WASM_SECOND_PHASE_ARGUMENTS = mapOf(
        "Xreport-all-warnings" to true,
        "Wextra" to true,
        "Xwasm" to true,
        "Xir-produce-js" to true,
        "Xir-dce" to true,
        "libraries" to Pair(
            kotlinEnvironment.WASM_LIBRARIES.sorted().joinToString(PATH_SEPARATOR) { it.split("/").last() },
            kotlinEnvironment.WASM_LIBRARIES.joinToString(PATH_SEPARATOR)
        ),
        "ir-output-name" to WASM_DEFAULT_MODULE_NAME,
    )

    val PREDEFINED_COMPOSE_WASM_FIRST_PHASE_ARGUMENTS = mapOf(
        "Xreport-all-warnings" to true,
        "Wextra" to true,
        "Xwasm" to true,
        "Xir-produce-klib-dir" to true,
        "libraries" to Pair(
            kotlinEnvironment.COMPOSE_WASM_LIBRARIES.sorted().joinToString(PATH_SEPARATOR) { it.split("/").last() },
            kotlinEnvironment.COMPOSE_WASM_LIBRARIES.joinToString(PATH_SEPARATOR)
        ),
        "ir-output-name" to WASM_DEFAULT_MODULE_NAME,
        "Xplugin" to Pair(
            kotlinEnvironment.COMPOSE_WASM_COMPILER_PLUGINS.map { it.split("/").last() },
            kotlinEnvironment.COMPOSE_WASM_COMPILER_PLUGINS
        ),
        "P" to Pair(
            kotlinEnvironment.composeWasmCompilerPluginOptions,
            kotlinEnvironment.composeWasmCompilerPluginOptions
        )
    )

    val PREDEFINED_COMPOSE_WASM_SECOND_PHASE_ARGUMENTS = mapOf(
        "Xreport-all-warnings" to true,
        "Wextra" to true,
        "Xwasm" to true,
        "Xir-produce-js" to true,
        "libraries" to Pair(
            kotlinEnvironment.COMPOSE_WASM_LIBRARIES.sorted().joinToString(PATH_SEPARATOR) { it.split("/").last() },
            kotlinEnvironment.COMPOSE_WASM_LIBRARIES.joinToString(PATH_SEPARATOR)
        ),
        "ir-output-name" to WASM_DEFAULT_MODULE_NAME,
        "Xwasm-included-module-only" to true,
    )

    val PREDEFINED_JS_FIRST_PHASE_ARGUMENTS = mapOf(
        "Xreport-all-warnings" to true,
        "Wextra" to true,
        "Xir-produce-klib-dir" to true,
        "libraries" to Pair(
            kotlinEnvironment.JS_LIBRARIES.sorted().joinToString(PATH_SEPARATOR) { it.split("/").last() },
            kotlinEnvironment.JS_LIBRARIES.joinToString(PATH_SEPARATOR)
        ),
        "ir-output-name" to JS_DEFAULT_MODULE_NAME,
    )

    val PREDEFINED_JS_SECOND_PHASE_ARGUMENTS = mapOf(
        "Xreport-all-warnings" to true,
        "Wextra" to true,
        "Xir-produce-js" to true,
        "Xir-dce" to true,
        "libraries" to Pair(
            kotlinEnvironment.JS_LIBRARIES.sorted().joinToString(PATH_SEPARATOR) { it.split("/").last() },
            kotlinEnvironment.JS_LIBRARIES.joinToString(PATH_SEPARATOR)
        ),
        "ir-output-name" to JS_DEFAULT_MODULE_NAME,
    )

    fun convertCompilerArgumentsToCompilationString(
        allArguments: Set<ExtendedCompilerArgument>,
        predefinedArguments: Map<String, Any>,
        userArguments: Map<String, Any>
    ): List<String> {
        return allArguments
            .flatMap {
                if (it.name in (predefinedArguments.keys + userArguments.keys))
                    if (!it.disabled && it.name in userArguments.keys) {
                        convertToCompilerArgumentsStringList(it.name, userArguments[it.name]!!)
                    } else {
                        convertToCompilerArgumentsStringList(
                            it.name,
                            (predefinedArguments[it.name] as? Pair<*, *>)?.second ?: predefinedArguments[it.name]
                        )
                    }
                else emptyList()
            }
            .map { it.filterNot { it.isWhitespace() } }

    }

    fun convertToCompilerArgumentsStringList(argumentName: String, argumentValue: Any?): List<String> {
        return when (argumentValue) {
            is Boolean -> if (argumentValue) listOf("-$argumentName") else emptyList()

            is String -> {
                if (argumentName == "XXLanguage") listOf("-$argumentName:$argumentValue".filterNot { it.isWhitespace() })
                else listOf("-$argumentName", argumentValue)
            }

            is List<*> -> argumentValue.map { "-$argumentName=$it" }
            else -> throw IllegalArgumentException("Unknown type of argument value: ${argumentValue?.javaClass?.name}")
        }
    }

    fun collectJvmArguments(
        kotlinTargetCompilerArgumentsByName: Map<String, Set<KotlinCompilerArgument>>
    ): Set<ExtendedCompilerArgument> {
        return kotlinTargetCompilerArgumentsByName[jvmCompilerArguments]?.processCompilerArgs(
            predefinedArguments = PREDEFINED_JVM_ARGUMENTS,
            allowedArguments = ALLOWED_JVM_ARGUMENTS,
        ) ?: throw IllegalStateException("Kotlin compiler arguments for JVM target not found")
    }

    fun collectWasmArguments(
        kotlinTargetCompilerArgumentsByName: Map<String, Set<KotlinCompilerArgument>>
    ): Set<ExtendedCompilerArgument> {
        return kotlinTargetCompilerArgumentsByName[wasmArguments]?.processCompilerArgs(
            predefinedArguments = PREDEFINED_WASM_FIRST_PHASE_ARGUMENTS,
            allowedArguments = ALLOWED_JS_ARGUMENTS
        ) ?: throw IllegalStateException("Kotlin compiler arguments for WASM target not found")

    }

    fun collectComposeWasmArguments(
        kotlinTargetCompilerArgumentsByName: Map<String, Set<KotlinCompilerArgument>>
    ): Set<ExtendedCompilerArgument> {
        return kotlinTargetCompilerArgumentsByName[wasmArguments]?.processCompilerArgs(
            predefinedArguments = PREDEFINED_COMPOSE_WASM_FIRST_PHASE_ARGUMENTS,
            allowedArguments = ALLOWED_WASM_ARGUMENTS
        ) ?: throw IllegalStateException("Kotlin compiler arguments for COMPOSE WASM target not found")

    }

    fun collectJsArguments(
        kotlinTargetCompilerArgumentsByName: Map<String, Set<KotlinCompilerArgument>>
    ): Set<ExtendedCompilerArgument> {
        return kotlinTargetCompilerArgumentsByName[jsArguments]?.processCompilerArgs(
            predefinedArguments = PREDEFINED_JS_FIRST_PHASE_ARGUMENTS,
            allowedArguments = ALLOWED_WASM_ARGUMENTS
        ) ?: throw IllegalStateException("Kotlin compiler arguments for JS target not found")

    }

    private fun Collection<KotlinCompilerArgument>.processCompilerArgs(
        allowedArguments: Set<String> = emptySet(),
        predefinedArguments: Map<String, Any?> = emptyMap()
    ): Set<ExtendedCompilerArgument> =
        map { arg ->
            val disabled = arg.name !in allowedArguments
            ExtendedCompilerArgument(
                name = arg.name,
                shortName = arg.shortName,
                description = arg.description.current,
                type = convertKotlinArgumentValueTypeToExtendedCompilerArgumentValue(arg.valueType),
                disabled = disabled,
                predefinedValues = (predefinedArguments[arg.name] as? Pair<*, *>)?.first
                    ?: predefinedArguments[arg.name] as? List<*>,
                supportedOnCurrentVersion = arg.isSupportedOnCurrentVersion()
            )
        }.toSet()

    private fun convertKotlinArgumentValueTypeToExtendedCompilerArgumentValue(type: KotlinArgumentValueType<*>): ExtendedCompilerArgumentValue<*> {
        return when (type) {
            is BooleanType -> {
                BooleanExtendedCompilerArgumentValue(
                    isNullable = type.isNullable.current,
                    defaultValue = type.defaultValue.current
                )
            }

            is StringType, is IntType -> {
                StringExtendedCompilerArgumentValue(
                    isNullable = type.isNullable.current,
                    defaultValue = type.defaultValue.current?.toString()
                )
            }

            is KotlinJvmTargetType -> {
                StringExtendedCompilerArgumentValue(
                    isNullable = type.isNullable.current,
                    defaultValue = type.defaultValue.current?.targetName
                )
            }

            is ReturnValueCheckerModeType -> {
                StringExtendedCompilerArgumentValue(
                    isNullable = type.isNullable.current,
                    defaultValue = type.defaultValue.current?.modeState
                )
            }

            is KotlinExplicitApiModeType -> {
                StringExtendedCompilerArgumentValue(
                    isNullable = type.isNullable.current,
                    defaultValue = type.defaultValue.current?.modeName
                )
            }

            is KotlinVersionType -> {
                StringExtendedCompilerArgumentValue(
                    isNullable = type.isNullable.current,
                    defaultValue = type.defaultValue.current?.versionName
                )
            }

            is KlibIrInlinerModeType -> {
                StringExtendedCompilerArgumentValue(
                    isNullable = type.isNullable.current,
                    defaultValue = type.defaultValue.current?.modeState
                )
            }

            is StringArrayType -> {
                ListExtendedCompilerArgumentValue(
                    isNullable = type.isNullable.current,
                    defaultValue = type.defaultValue.current?.toList() ?: emptyList<Any>()
                )
            }

            is KotlinHeaderModeType -> {
                StringExtendedCompilerArgumentValue(
                    isNullable = type.isNullable.current,
                    defaultValue = type.defaultValue.current?.modeName
                )
            }
            // TODO(Dmitrii Krasnov): remove else branch when KT-83794 is finished
            else -> {
                StringExtendedCompilerArgumentValue(
                    isNullable = type.isNullable.current,
                    defaultValue = type.defaultValue.current?.toString()
                )
            }
        }
    }

    private fun KotlinCompilerArgument.isSupportedOnCurrentVersion(): Boolean {
        return releaseVersionsMetadata.removedVersion?.releaseName?.let { releaseVersion ->
            KotlinToolingVersion(versionInfo.version) < KotlinToolingVersion(releaseVersion)
        } ?: true
    }
}