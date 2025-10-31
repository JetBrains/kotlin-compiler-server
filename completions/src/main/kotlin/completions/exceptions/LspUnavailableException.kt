package completions.exceptions

import java.lang.RuntimeException

class LspUnavailableException(override val message: String = "LSP service unavailable"): RuntimeException()