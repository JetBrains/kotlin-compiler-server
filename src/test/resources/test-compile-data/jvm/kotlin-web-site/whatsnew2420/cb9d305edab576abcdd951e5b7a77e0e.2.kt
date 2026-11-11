@OptIn(ExperimentalStdlibApi::class)
fun main() {
    data class Response(
        val participantId: String,
        val answer: String,
        val responseDate: String
    )

    val responses = listOf(
        Response("P001", "Yes", "2026-07-21"),
        Response("P002", "Maybe", "2026-07-21"),
        Response("P003", "No", "2026-07-21")
    )

    // Checks if all participants gave the same answer
    println(responses.allEqualBy { it.answer })
    // false

    // Checks for duplicate participants
    println(responses.allDistinctBy { it.participantId })
    // true

    // Checks if all responses were submitted on the same date
    println(responses.allEqualBy { it.responseDate })
    // true

    val answers = responses.map { it.answer }

    // Checks if answers are identical
    println(answers.allEqual())
    // false

    // Checks if answers are distinct
    println(answers.allDistinct())
    // true
}