package com.compiler.server.model

import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.ir.backend.js.ModulesStructure

interface Analysis {
    val componentProvider: ComponentProvider
    val analysisResult: AnalysisResult
}

data class AnalysisJvm(
    override val componentProvider: ComponentProvider,
    override val analysisResult: AnalysisResult
) : Analysis

data class AnalysisJs(
    val sourceModule: ModulesStructure,
    override val componentProvider: ComponentProvider,
    override val analysisResult: AnalysisResult
) : Analysis