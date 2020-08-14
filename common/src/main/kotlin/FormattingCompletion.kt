package common

private val NUMBER_OF_CHAR_IN_TAIL = 40
private val NUMBER_OF_CHAR_IN_COMPLETION_NAME = 60

private fun formatName(
  builder: String,
  symbols: Int
) = if (builder.length > symbols) builder.substring(0, symbols) + "..." else builder

fun formatCompletionName(
  builder: String
) = formatName(builder, NUMBER_OF_CHAR_IN_COMPLETION_NAME)

fun formatTailName(
  builder: String
) = formatName(builder, NUMBER_OF_CHAR_IN_TAIL)