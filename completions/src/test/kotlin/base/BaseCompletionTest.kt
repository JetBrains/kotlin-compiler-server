package base

interface BaseCompletionTest {
    fun performCompletionChecks(
        code: String,
        line: Int,
        character: Int,
        completions: List<String>,
        isJs: Boolean = false
    )
}