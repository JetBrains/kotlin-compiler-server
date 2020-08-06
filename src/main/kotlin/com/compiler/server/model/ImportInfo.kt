package com.compiler.server.model

data class ImportInfo (val importName: String,
                       val shortName: String,
                       val fullName: String,
                       val returnType: String,
                       val icon: String)