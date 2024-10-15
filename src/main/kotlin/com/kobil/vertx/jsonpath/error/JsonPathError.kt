package com.kobil.vertx.jsonpath.error

import com.kobil.vertx.jsonpath.compiler.Token
import kotlinx.coroutines.CancellationException

/**
 * The base type for JSON Path compiler errors.
 */
sealed interface JsonPathError {
  /**
   * The input (JSON Path string) ended unexpectedly.
   *
   * @param expected the expected element when the EOF occurred
   * @param line the line number at which the string ended
   * @param column the column number within the line at which the string ended
   */
  data class UnexpectedEof(
    val expected: String,
    val line: UInt,
    val column: UInt,
  ) : JsonPathError {
    /**
     * Serializes this error to a readable string message
     */
    override fun toString(): String =
      "${messagePrefix(line, column)}: Premature end of input, expected $expected"
  }

  /**
   * The compiler encountered a character in the input string that was invalid in the current
   * context.
   *
   * @param char the illegal character
   * @param line the line of the invalid character
   * @param column the column of the invalid character
   * @param reason information why the character was invalid
   */
  data class IllegalCharacter(
    val char: Char,
    val line: UInt,
    val column: UInt,
    val reason: String,
  ) : JsonPathError {
    /**
     * Serializes this error to a readable string message
     */
    override fun toString(): String =
      "${messagePrefix(line, column)}: Illegal character '$char' ($reason)"
  }

  /**
   * Some quoted string was missing the closing quote.
   *
   * @param line the line where the error was detected
   * @param column the column where the error was detected
   * @param startLine the line where the opening quote was located
   * @param startColumn the column where the opening quote was located
   */
  data class UnterminatedString(
    val line: UInt,
    val column: UInt,
    val startLine: UInt,
    val startColumn: UInt,
  ) : JsonPathError {
    /**
     * Serializes this error to a readable string message
     */
    override fun toString(): String =
      "${
        messagePrefix(
          line,
          column,
        )
      }: Unterminated string literal starting at [$startLine:$startColumn]"
  }

  /**
   * An integer value outside the supported interval of -2^31..2^31 - 1 was encountered. While the
   * JSON Path specification allows indices to be 48 bit integers, this isn't possible in Java.
   *
   * @param string the string representation of the integer
   * @param line the line where the integer is located
   * @param column the column where the integer is located
   */
  data class IntOutOfBounds(
    val string: String,
    val line: UInt,
    val column: UInt,
  ) : JsonPathError {
    /**
     * Serializes this error to a readable string message
     */
    override fun toString(): String =
      "${
        messagePrefix(
          line,
          column,
        )
      }: Invalid integer value (Out of bounds)"
  }

  /**
   * An unexpected token was encountered in the current context.
   *
   * @param token a readable name of the token
   * @param line the starting line of the token
   * @param column the starting column of the token
   * @param context the current parsing context when the error occurred
   */
  data class UnexpectedToken(
    val token: String,
    val line: UInt,
    val column: UInt,
    val context: String,
  ) : JsonPathError {
    internal constructor(token: Token, parsing: String) : this(
      token.name,
      token.line,
      token.column,
      parsing,
    )

    /**
     * Serializes this error to a readable string message
     */
    override fun toString(): String =
      "${
        messagePrefix(
          line,
          column,
        )
      }: Unexpected token '$token' while parsing $context"
  }

  /**
   * An illegal selector was encountered. This could be an index or slice selector in a dotted
   * segment or a non-quoted string in a bracketed segment.
   *
   * @param line the line at which the error was detected
   * @param column the column at which the error was detected
   * @param reason a more detailed description of the error
   */
  data class IllegalSelector(
    val line: UInt,
    val column: UInt,
    val reason: String,
  ) : JsonPathError {
    /**
     * Serializes this error to a readable string message
     */
    override fun toString(): String = "${messagePrefix(line, column)}: Illegal selector ($reason)"
  }

  /**
   * The compiler encountered a function extension that was unknown.
   *
   * @param name the name of the unknown function
   * @param line the line at which the unknown function occurred
   * @param column the column at which the unknown function occurred
   */
  data class UnknownFunction(
    val name: String,
    val line: UInt,
    val column: UInt,
  ) : JsonPathError {
    /**
     * Serializes this error to a readable string message
     */
    override fun toString(): String =
      "${messagePrefix(line, column)}: Unknown function extension '$name'"
  }

  /**
   * A non-singular query appeared in a context where a singular query was expected (e.g. an operand
   * of a comparison)
   *
   * @param line the line at which the offending query occurred
   * @param column the column at which the offending query occurred
   */
  data class MustBeSingularQuery(
    val line: UInt,
    val column: UInt,
  ) : JsonPathError {
    /**
     * Serializes this error to a readable string message
     */
    override fun toString(): String = "${messagePrefix(line, column)}: A singular query is expected"
  }

  /**
   * An invalid escape sequence occurred in a string literal.
   *
   * @param string the string literal
   * @param line the line at which the string literal is located
   * @param column the column at which the string literal is located
   * @param position the position of the invalid escape sequence within the string
   * @param reason a more detailed description of the issue
   */
  data class InvalidEscapeSequence(
    val string: String,
    val line: UInt,
    val column: UInt,
    val position: UInt,
    val reason: String,
  ) : JsonPathError {
    /**
     * Serializes this error to a readable string message
     */
    override fun toString(): String =
      "${messagePrefix(line, column)}: " +
        "Invalid escape sequence at position $position in string literal '$string' ($reason)"
  }

  /**
   * Contains helpers and a constructor-like factory function
   */
  companion object {
    /**
     * Creates a [JsonPathError] from the given [Throwable]. If it is a [JsonPathException], the
     * contained error is unwrapped.
     *
     * @param throwable the throwable to convert
     * @return a corresponding JSON Path Error
     */
    operator fun invoke(throwable: Throwable): JsonPathError =
      when (throwable) {
        is JsonPathException -> throwable.err
        is CancellationException, is Error -> throw throwable
        else -> throw IllegalStateException(throwable)
      }

    private fun messagePrefix(
      line: UInt,
      column: UInt,
    ): String = "Error at [$line:$column]"
  }
}
