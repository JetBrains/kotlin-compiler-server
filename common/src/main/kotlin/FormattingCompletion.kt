package common

val NUMBER_OF_CHAR_IN_TAIL = 40
val NUMBER_OF_CHAR_IN_COMPLETION_NAME = 60

fun formatName(
  builder: String,
  symbols: Int
) = if (builder.length > symbols) builder.substring(0, symbols) + "..." else builder