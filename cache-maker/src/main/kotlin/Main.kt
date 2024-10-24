package cache

import CacheBuilder
import com.compiler.server.common.components.KotlinEnvironmentConfiguration

fun main(args: Array<String>) {
  val version = args[0]
  val directory = args[1]
  val outputPathCacheComposeWasm = args[2]
  val kotlinEnvironment = KotlinEnvironmentConfiguration(version, directory).kotlinEnvironment

  CacheBuilder(
    kotlinEnvironment = kotlinEnvironment
  ).compileCache(outputPathCacheComposeWasm)
}
