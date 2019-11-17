package com.compiler.server.model

import java.nio.file.Path

data class OutputDirectory(val path: Path, val subPaths: List<Path>)