package com.compiler.server.validation.impl

import com.compiler.server.model.ExtendedCompilerArgument
import com.compiler.server.validation.AbstractCompilerArgumentsValidator
import org.springframework.stereotype.Component

@Component
class JsCompilerArgumentsValidator(jsCompilerArguments: Set<ExtendedCompilerArgument>) :
    AbstractCompilerArgumentsValidator(jsCompilerArguments)