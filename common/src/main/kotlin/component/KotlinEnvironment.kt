package component

import java.io.File

class KotlinEnvironment(
  val classpath: List<File>,
  additionalJsClasspath: List<File>,
  additionalWasmClasspath: List<File>,
) {
  companion object {
    /**
     * This list allows to configure behavior of webdemo compiler. Its effect is equivalent
     * to passing this list of string to CLI compiler.
     *
     * See [org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments] and
     * [org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments] for list of possible flags
     */
    val additionalCompilerArguments: List<String> = listOf(
      "-opt-in=kotlin.ExperimentalStdlibApi",
      "-opt-in=kotlin.time.ExperimentalTime",
      "-opt-in=kotlin.RequiresOptIn",
      "-opt-in=kotlin.ExperimentalUnsignedTypes",
      "-opt-in=kotlin.contracts.ExperimentalContracts",
      "-opt-in=kotlin.experimental.ExperimentalTypeInference",
      "-Xcontext-receivers",
    )
  }

  val JS_LIBRARIES = additionalJsClasspath.map { it.absolutePath }
  val WASM_LIBRARIES = additionalWasmClasspath.map { it.absolutePath }

}
