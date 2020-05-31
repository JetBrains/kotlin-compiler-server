package com.compiler.server

import io.kotless.dsl.spring.Kotless
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import kotlin.reflect.KClass

@SpringBootApplication
class CompilerApplication : Kotless() {
    override val bootKlass: KClass<*> = this::class
}

fun main(args: Array<String>) {
    runApplication<CompilerApplication>(*args)
}
