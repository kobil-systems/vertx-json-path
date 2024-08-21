package com.kobil.vertx.jsonpath.compiler

import arrow.core.raise.Raise
import arrow.core.raise.recover
import com.kobil.vertx.jsonpath.error.JsonPathError
import com.kobil.vertx.jsonpath.error.JsonPathException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow

internal data class JsonPathScanner(
  val expr: String,
) {
  fun tokens(): Flow<Token> =
    flow {
      recover(
        block = {
          val state = State(this@flow, this)

          if (expr.firstOrNull()?.isWhitespace() == true) {
            raise(JsonPathError.IllegalCharacter(expr[0], 1, 1, "Leading whitespace is disallowed"))
          }

          while (state.current < expr.length) {
            state.start = state.current
            state.startLine = state.line
            state.startColumn = state.column
            emit(state.scanToken())
          }

          if (expr.lastOrNull()?.isWhitespace() == true) {
            raise(
              JsonPathError.IllegalCharacter(
                expr.last(),
                state.line,
                state.column - 1,
                "Trailing whitespace is disallowed",
              ),
            )
          }

          emit(Token.Eof(state.line, state.column))
        },
        recover = {
          throw JsonPathException(it)
        },
      )
    }

  private fun State.scanToken(): Token =
    when (val ch = advance()) {
      // Simple one-character tokens
      '$' -> Token.Dollar(line, column)
      '@' -> Token.At(line, column)
      ',' -> Token.Comma(line, column)
      '*' -> Token.Star(line, column)
      ':' -> Token.Colon(line, column)
      '?' -> Token.QuestionMark(line, column)
      '[' -> Token.LeftBracket(line, column)
      ']' -> Token.RightBracket(line, column)
      '(' -> Token.LeftParen(line, column)
      ')' -> Token.RightParen(line, column)

      // Potential two-character tokens
      '.' -> match('.', { Token.DotDot(line, column) }, { Token.Dot(line, column) })
      '!' -> match('=', { Token.BangEq(line, column) }, { Token.Bang(line, column) })
      '<' -> match('=', { Token.LessEq(line, column) }, { Token.Less(line, column) })
      '>' -> match('=', { Token.GreaterEq(line, column) }, { Token.Greater(line, column) })

      // Two-character tokens
      '=' -> expect('=') { Token.EqEq(line, column) }
      '&' -> expect('&') { Token.AndAnd(line, column) }
      '|' -> expect('|') { Token.PipePipe(line, column) }

      // Numbers
      in '0'..'9', '-' -> scanNumber(ch)

      // Strings
      '\'', '\"' -> scanQuotedString(ch)

      // Potential name shorthand
      in 'A'..'Z', in 'a'..'z', '_', in '\u0080'..'\ud7ff', in '\ue000'..'\uffff' ->
        scanIdentifier()

      ' ', '\t', '\r' -> scanWhitespace(line, column)
      '\n' -> {
        val l = line
        val c = column

        newLine()
        scanWhitespace(l, c)
      }

      else ->
        raise(
          JsonPathError.IllegalCharacter(
            ch,
            line,
            column - 1,
            "Character not allowed in JSON Path",
          ),
        )
    }

  private fun State.scanNumber(first: Char): Token.Num {
    val negative = first == '-'

    when {
      negative && !expr[current].isDigit() ->
        raise(
          JsonPathError.IllegalCharacter(
            expr[current],
            line,
            column,
            "Expecting an integer part after '-'",
          ),
        )

      first == '0' && !isAtEnd() && expr[current].isDigit() ->
        raise(
          JsonPathError.IllegalCharacter(
            expr[current],
            line,
            column - 1,
            "Leading zeroes in numbers are not allowed",
          ),
        )

      first == '-' && !isAtEnd(1) && expr[current] == '0' && expr[current + 1].isDigit() ->
        raise(
          JsonPathError.IllegalCharacter(
            expr[current],
            line,
            column,
            "Leading zeroes in numbers are not allowed",
          ),
        )
    }

    while (!isAtEnd() && expr[current].isDigit()) advance()

    var decimal = false
    if (!isAtEnd(1) && expr[current] == '.' && expr[current + 1].isDigit()) {
      decimal = true
      advance()

      while (!isAtEnd() && expr[current].isDigit()) advance()
    }

    if (!isAtEnd(2) &&
      expr[current] in exponent &&
      (
        (expr[current + 1] in plusMinus && expr[current + 2].isDigit()) ||
          expr[current + 1].isDigit()
      )
    ) {
      decimal = true
      advance()
      if (expr[current] in plusMinus) advance()

      while (!isAtEnd() && expr[current].isDigit()) advance()
    }

    val strValue =
      if (negative) {
        expr.substring(start + 1..<current)
      } else {
        expr.substring(
          start..<current,
        )
      }
    val value = if (decimal) strValue.toDouble() else strValue.toInt()

    return Token.Num(line, column, negative, value)
  }

  private fun State.scanQuotedString(quote: Char): Token.Str {
    var escaped = false

    while (!isAtEnd() && (expr[current] != quote || escaped)) {
      val ch = advance()

      if (escaped && quote == '"' && ch == '\'') {
        raise(
          JsonPathError.IllegalCharacter(
            ch,
            line,
            column,
            "In a double-quoted string, single quotes can't be escaped",
          ),
        )
      } else if (escaped && quote == '\'' && ch == '"') {
        raise(
          JsonPathError.IllegalCharacter(
            ch,
            line,
            column,
            "In a single-quoted string, double quotes can't be escaped",
          ),
        )
      }

      if (ch == '\\' && !escaped) {
        escaped = true
      } else {
        if (ch == '\n') newLine()

        escaped = false
      }
    }

    if (isAtEnd()) raise(JsonPathError.UnterminatedString(line, column, startLine, startColumn))

    advance()

    return Token.Str(line, column, expr.substring(start + 1..<current - 1))
  }

  private fun State.scanIdentifier(): Token.Identifier {
    while (!isAtEnd() && expr[current] in identifierChar) {
      advance()
    }

    return Token.Identifier(line, column, expr.substring(start..<current))
  }

  private fun State.scanWhitespace(
    line: Int,
    column: Int,
  ): Token.Whitespace {
    while (!isAtEnd() && expr[current] in whitespaceChar) {
      if (advance() == '\n') newLine()
    }

    return Token.Whitespace(line, column)
  }

  private fun State.isAtEnd(distance: Int = 0): Boolean = current + distance >= expr.length

  private fun State.advance(): Char = expr[current++]

  private fun State.advanceIf(next: Char): Boolean =
    if (!isAtEnd() && expr[current] == next) {
      true.also { current++ }
    } else {
      false
    }

  private fun State.match(
    next: Char,
    ifMatch: () -> Token,
    ifNoMatch: () -> Token,
  ): Token = if (advanceIf(next)) ifMatch() else ifNoMatch()

  private fun State.expect(
    next: Char,
    ifMatch: () -> Token,
  ): Token =
    if (advanceIf(next)) {
      ifMatch()
    } else {
      raise(JsonPathError.IllegalCharacter(expr[current], line, column, "Expected '$next'"))
    }

  private fun State.newLine() {
    line++
    currentLineStart = current
  }

  private val State.column: Int
    get() = start - currentLineStart + 1

  class State(
    flow: FlowCollector<Token>,
    raise: Raise<JsonPathError>,
    var start: Int = 0,
    var current: Int = 0,
    var line: Int = 1,
    var currentLineStart: Int = 0,
    var startLine: Int = 1,
    var startColumn: Int = 1,
  ) : Raise<JsonPathError> by raise,
    FlowCollector<Token> by flow

  companion object {
    private val identifierChar =
      ('A'..'Z').toSet() + ('a'..'z') + '_' + ('\u0080'..'\ud7ff') + ('\ue000'..'\uffff') +
        ('0'..'9')
    private val whitespaceChar = setOf(' ', '\t', '\n', '\r')
    private val plusMinus = setOf('+', '-')
    private val exponent = setOf('e', 'E')
  }
}
