package com.kobil.vertx.jsonpath.error

import com.kobil.vertx.jsonpath.compiler.Token

sealed interface JsonPathError {
  data class IllegalCharacter(
    val char: Char,
    val line: Int,
    val column: Int,
    val reason: String,
  ) : JsonPathError {
    override fun toString(): String =
      "${messagePrefix(line, column)}: Illegal character '$char' ($reason)"
  }

  data class UnterminatedString(
    val line: Int,
    val column: Int,
    val startLine: Int,
    val startColumn: Int,
  ) : JsonPathError {
    override fun toString(): String =
      "${messagePrefix(
        line,
        column,
      )}: Unterminated string literal starting at [$startLine:$startColumn]"
  }

  data class UnexpectedToken(
    val token: Token,
    val parsing: String,
  ) : JsonPathError {
    override fun toString(): String =
      "${messagePrefix(token.line, token.column)}: Unexpected token while parsing $parsing"
  }

  data class IllegalSelector(
    val line: Int,
    val column: Int,
    val reason: String,
  ) : JsonPathError {
    override fun toString(): String = "${messagePrefix(line, column)}: Illegal selector ($reason)"
  }

  data class UnknownFunction(
    val name: String,
    val line: Int,
    val column: Int,
  ) : JsonPathError {
    override fun toString(): String =
      "${messagePrefix(line, column)}: Unknown function extension '$name'"
  }

  data class MustBeSingularQuery(
    val line: Int,
    val column: Int,
  ) : JsonPathError {
    override fun toString(): String = "${messagePrefix(line, column)}: A singular query is expected"
  }

  data class IndicesMustBeIntegers(
    val number: Number,
    val line: Int,
    val column: Int,
  ) : JsonPathError {
    override fun toString(): String =
      "${messagePrefix(line, column)}: Expected an integer index, but got $number"
  }

  data class InvalidEscapeSequence(
    val string: String,
    val line: Int,
    val column: Int,
    val position: Int,
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
        else -> UnexpectedError(throwable)
      }

    private fun messagePrefix(
      line: Int,
      column: Int,
    ): String = "Error at [$line:$column]"
  }
}
