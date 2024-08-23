package com.kobil.vertx.jsonpath.compiler

import arrow.core.raise.Raise
import com.kobil.vertx.jsonpath.error.JsonPathError
import java.nio.charset.StandardCharsets

private val INVALID_ESCAPE = """(?<!\\)\\(?!(?>u[a-fA-F0-9]{4}|[btnfr'"/\\]))""".toRegex()
private val CONTROL_CHARACTER = """[\u0000-\u001F]""".toRegex()
private val INVALID_SURROGATE_PAIR =
  """(?>\\u[Dd][89aAbB][0-9a-fA-F]{2}(?!\\u[Dd][CcDdEeFf][0-9a-fA-F]{2}))""".toRegex()
private val LONE_LOW_SURROGATE =
  """(?>(?<!\\u[Dd][89aAbB][0-9a-fA-F]{2})\\u[Dd][CcDdEeFf][0-9a-fA-F]{2})""".toRegex()

private val UNICODE_SEQUENCE = """(\\u[0-9a-fA-F]{4})+""".toRegex()

internal fun Raise<JsonPathError.InvalidEscapeSequence>.unescape(rawName: Token.Str): String {
  check(rawName, INVALID_ESCAPE) { "Invalid escape sequence" }
  check(rawName, INVALID_SURROGATE_PAIR) {
    "A unicode high surrogate must be followed by a low surrogate"
  }
  check(rawName, LONE_LOW_SURROGATE) {
    "A unicode low surrogate must be preceded by a high surrogate"
  }
  check(rawName, CONTROL_CHARACTER) { "Control characters (U+0000 to U+001F) are disallowed" }

  return rawName
    .value
    .replace("\\b", "\b")
    .replace("\\t", "\t")
    .replace("\\n", "\n")
    .replace("\\f", "\u000c")
    .replace("\\r", "\r")
    .replace("\\\"", "\"")
    .replace("\\'", "'")
    .replace("\\/", "/")
    .unescapeUnicode()
    .replace("\\\\", "\\")
}

@OptIn(ExperimentalStdlibApi::class)
private fun String.unescapeUnicode(): String {
  var result = this
  var match = UNICODE_SEQUENCE.find(result)

  while (match != null) {
    val sequence = match.value

    val decoded =
      sequence
        .replace("\\u", "")
        .lowercase()
        .hexToByteArray()
        .toString(StandardCharsets.UTF_16)

    result =
      result.substring(0..<match.range.first) + decoded +
      result.substring(match.range.last + 1..<result.length)
    match = UNICODE_SEQUENCE.find(result)
  }

  return result
}

private fun Raise<JsonPathError.InvalidEscapeSequence>.check(
  rawName: Token.Str,
  regex: Regex,
  reason: () -> String,
) {
  regex.find(rawName.value)?.let {
    raise(
      JsonPathError.InvalidEscapeSequence(
        rawName.value,
        rawName.line,
        rawName.column,
        it.range.first.toUInt(),
        reason(),
      ),
    )
  }
}
