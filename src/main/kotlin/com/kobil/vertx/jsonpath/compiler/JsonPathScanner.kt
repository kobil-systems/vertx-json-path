package com.kobil.vertx.jsonpath.compiler

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.Raise
import arrow.core.raise.recover
import arrow.core.right
import com.kobil.vertx.jsonpath.error.JsonPathError

internal typealias TokenEvent = Either<JsonPathError, Token>

internal fun String.scanTokens(): Sequence<TokenEvent> =
  sequence {
    recover(
      block = {
        val scannerState = ScannerState(this@scanTokens, this)

        if (firstOrNull() in whitespaceChar) {
          this.raise(
            JsonPathError.IllegalCharacter(
              this@scanTokens[0],
              1U,
              1U,
              "Leading whitespace is disallowed",
            ),
          )
        }

        while (scannerState.current < uLength) {
          scannerState.start = scannerState.current
          scannerState.startLine = scannerState.line
          scannerState.startColumn = scannerState.column
          yield(scannerState.scanToken().right())
        }

        if (lastOrNull() in whitespaceChar) {
          this.raise(
            JsonPathError.IllegalCharacter(
              last(),
              scannerState.line,
              scannerState.column - 1U,
              "Trailing whitespace is disallowed",
            ),
          )
        }

        scannerState.start = scannerState.current

        yield(Token.Eof(scannerState.line, scannerState.column).right())
      },
      recover = {
        yield(it.left())
      },
    )
  }

private fun ScannerState.scanToken(): Token =
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
          column - 1U,
          "Character not allowed in JSON Path",
        ),
      )
  }

private fun ScannerState.scanNumber(first: Char): Token {
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
          column - 1U,
          "Leading zeroes in numbers are not allowed",
        ),
      )

    first == '-' && !isAtEnd(1U) && expr[current] == '0' && expr[current + 1U].isDigit() ->
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

  if (!isAtEnd(1U) && expr[current] == '.' && expr[current + 1U].isDigit()) {
    return scanDecimalPart()
  }

  if (!isAtEnd(2U) && hasExponent) {
    return scanScientificNotation()
  }

  val strValue = expr.substring(start..<current)

  return Token.Integer(
    line,
    column,
    negative,
    strValue.toIntOrNull() ?: raise(JsonPathError.IntOutOfBounds(strValue, line, column)),
  )
}

private fun ScannerState.scanDecimalPart(): Token.Decimal {
  // Consume decimal dot
  advance()
  while (!isAtEnd() && expr[current].isDigit()) advance()

  if (!isAtEnd(2U) && hasExponent) {
    return scanScientificNotation()
  }

  return decimal
}

private fun ScannerState.scanScientificNotation(): Token.Decimal {
  // Consume exponent e and optional +/-
  advance()
  if (expr[current] in plusMinus) advance()

  while (!isAtEnd() && expr[current].isDigit()) advance()

  return decimal
}

private inline val ScannerState.hasExponent: Boolean
  get() =
    expr[current] in exponent &&
      (
        (expr[current + 1U] in plusMinus && expr[current + 2U].isDigit()) ||
          expr[current + 1U].isDigit()
      )

private inline val ScannerState.decimal: Token.Decimal
  get() = Token.Decimal(line, column, expr.substring(start..<current).toDouble())

private fun ScannerState.scanQuotedString(quote: Char): Token.Str {
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

  return Token.Str(line, column, expr.substring(start + 1U..<current - 1U))
}

private fun ScannerState.scanIdentifier(): Token.Identifier {
  while (!isAtEnd() && expr[current] in identifierChar) {
    advance()
  }

  return Token.Identifier(line, column, expr.substring(start..<current))
}

private fun ScannerState.scanWhitespace(
  line: UInt,
  column: UInt,
): Token.Whitespace {
  while (!isAtEnd() && expr[current] in whitespaceChar) {
    if (advance() == '\n') newLine()
  }

  return Token.Whitespace(line, column)
}

private fun ScannerState.isAtEnd(distance: UInt = 0U): Boolean = current + distance >= expr.uLength

private fun ScannerState.advance(): Char = expr[current++]

private fun ScannerState.advanceIf(next: Char): Boolean =
  if (!isAtEnd() && expr[current] == next) {
    true.also { current++ }
  } else {
    false
  }

private fun ScannerState.match(
  next: Char,
  ifMatch: () -> Token,
  ifNoMatch: () -> Token,
): Token = if (advanceIf(next)) ifMatch() else ifNoMatch()

private fun ScannerState.expect(
  next: Char,
  ifMatch: () -> Token,
): Token =
  if (advanceIf(next)) {
    ifMatch()
  } else if (isAtEnd()) {
    raise(JsonPathError.PrematureEof("'$next'", line, column))
  } else {
    raise(JsonPathError.IllegalCharacter(expr[current], line, column, "Expected '$next'"))
  }

private fun ScannerState.newLine() {
  line++
  currentLineStart = current
}

private val ScannerState.column: UInt
  get() = start - currentLineStart + 1U

private operator fun String.get(idx: UInt): Char = this[idx.toInt()]

private fun String.substring(range: UIntRange): String =
  substring(range.first.toInt()..range.last.toInt())

private val String.uLength: UInt
  get() = length.toUInt()

private class ScannerState(
  var expr: String,
  raise: Raise<JsonPathError>,
  var start: UInt = 0U,
  var current: UInt = 0U,
  var line: UInt = 1U,
  var currentLineStart: UInt = 0U,
  var startLine: UInt = 1U,
  var startColumn: UInt = 1U,
) : Raise<JsonPathError> by raise

private val identifierChar =
  ('A'..'Z').toSet() + ('a'..'z') + '_' + ('\u0080'..'\ud7ff') + ('\ue000'..'\uffff') +
    ('0'..'9')
private val whitespaceChar = setOf(' ', '\t', '\n', '\r')
private val plusMinus = setOf('+', '-')
private val exponent = setOf('e', 'E')
