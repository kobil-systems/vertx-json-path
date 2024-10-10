package com.kobil.vertx.jsonpath.error

import com.kobil.vertx.jsonpath.compiler.Token
import kotlinx.coroutines.CancellationException

sealed interface JsonPathError {
  data class PrematureEof(
    val expected: String,
    val line: UInt,
    val column: UInt,
  ) : JsonPathError {
    override fun toString(): String =
      "${messagePrefix(line, column)}: Premature end of input, expected $expected"
  }

  data class IllegalCharacter(
    val char: Char,
    val line: UInt,
    val column: UInt,
    val reason: String,
  ) : JsonPathError {
    override fun toString(): String =
      "${messagePrefix(line, column)}: Illegal character '$char' ($reason)"
  }

  data class UnterminatedString(
    val line: UInt,
    val column: UInt,
    val startLine: UInt,
    val startColumn: UInt,
  ) : JsonPathError {
    override fun toString(): String =
      "${
        messagePrefix(
          line,
          column,
        )
      }: Unterminated string literal starting at [$startLine:$startColumn]"
  }

  data class IntOutOfBounds(
    val string: String,
    val line: UInt,
    val column: UInt,
  ) : JsonPathError {
    override fun toString(): String =
      "${
        messagePrefix(
          line,
          column,
        )
      }: Invalid integer value (Out of bounds)"
  }

  data class UnexpectedToken(
    val token: String,
    val line: UInt,
    val column: UInt,
    val parsing: String,
  ) : JsonPathError {
    internal constructor(token: Token, parsing: String) : this(
      token.name,
      token.line,
      token.column,
      parsing
    )

    override fun toString(): String =
      "${
        messagePrefix(
          line,
          column,
        )
      }: Unexpected token '$token' while parsing $parsing"
  }

  data class IllegalSelector(
    val line: UInt,
    val column: UInt,
    val reason: String,
  ) : JsonPathError {
    override fun toString(): String = "${messagePrefix(line, column)}: Illegal selector ($reason)"
  }

  data class UnknownFunction(
    val name: String,
    val line: UInt,
    val column: UInt,
  ) : JsonPathError {
    override fun toString(): String =
      "${messagePrefix(line, column)}: Unknown function extension '$name'"
  }

  data class MustBeSingularQuery(
    val line: UInt,
    val column: UInt,
  ) : JsonPathError {
    override fun toString(): String = "${messagePrefix(line, column)}: A singular query is expected"
  }

  data class InvalidEscapeSequence(
    val string: String,
    val line: UInt,
    val column: UInt,
    val position: UInt,
    val reason: String,
  ) : JsonPathError {
    override fun toString(): String =
      "${messagePrefix(line, column)}: " +
        "Invalid escape sequence at position $position in string literal '$string' ($reason)"
  }

  data class UnexpectedError(
    val cause: Throwable,
  ) : JsonPathError {
    override fun toString(): String = "Unexpected error: $cause"
  }

  companion object {
    operator fun invoke(throwable: Throwable): JsonPathError =
      when (throwable) {
        is JsonPathException -> throwable.err
        is CancellationException, is Error -> throw throwable
        else -> UnexpectedError(throwable)
      }

    private fun messagePrefix(
      line: UInt,
      column: UInt,
    ): String = "Error at [$line:$column]"
  }
}
