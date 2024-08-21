package com.kobil.vertx.jsonpath.compiler

import arrow.core.Either
import arrow.core.nonEmptyListOf
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.kobil.vertx.jsonpath.ComparableExpression
import com.kobil.vertx.jsonpath.FilterExpression
import com.kobil.vertx.jsonpath.FunctionExpression
import com.kobil.vertx.jsonpath.JsonPath
import com.kobil.vertx.jsonpath.NodeListExpression
import com.kobil.vertx.jsonpath.QueryExpression
import com.kobil.vertx.jsonpath.Segment
import com.kobil.vertx.jsonpath.Selector
import com.kobil.vertx.jsonpath.Selector.Name.Companion.unescape
import com.kobil.vertx.jsonpath.error.JsonPathError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach

internal data class JsonPathParser(
  val scanner: JsonPathScanner,
  val scope: CoroutineScope,
) {
  suspend fun query(): Either<JsonPathError, JsonPath> =
    either {
      val ch = tokenChannel()

      try {
        State(ch, this).run {
          require<Token.Dollar>("JSON path query")
          JsonPath(segments())
        }
      } finally {
        ch.cancel()
      }
    }

  suspend fun filterExpression(): Either<JsonPathError, FilterExpression> =
    either {
      val ch = tokenChannel()

      try {
        State(ch, this).filterExpr()
      } finally {
        ch.cancel()
      }
    }

  private fun tokenChannel(): ReceiveChannel<Token> =
    Channel<Token>().also { ch ->
      scanner
        .tokens()
        .catch { ch.close(it) }
        .onCompletion { ch.close(it) }
        .onEach(ch::send)
        .launchIn(scope)
    }

  private suspend fun State.filterExpr() = or()

  private suspend fun State.segments(): List<Segment> =
    buildList {
      while (!isAtEnd()) {
        skipWhiteSpace()

        val dot = takeIf<Token.Dot>()

        if (dot != null || advanceIf<Token.LeftBracket>()) {
          add(childSegment(dot != null))
        } else if (advanceIf<Token.DotDot>()) {
          add(descendantSegment())
        } else {
          break
        }
      }
    }.toList()

  private suspend fun State.childSegment(dottedSegment: Boolean): Segment =
    Segment.ChildSegment(selectors(dottedSegment, "child"))

  private suspend fun State.descendantSegment(): Segment {
    val dottedSegment = !advanceIf<Token.LeftBracket>()
    return Segment.DescendantSegment(selectors(dottedSegment, "descendant"))
  }

  private suspend fun State.selectors(
    dottedSegment: Boolean,
    segmentType: String,
  ): List<Selector> {
    val selectors = mutableListOf<Selector>()

    do {
      if (!dottedSegment) skipWhiteSpace()
      selectors.add(selector(dottedSegment))
      if (!dottedSegment) skipWhiteSpace()
    } while (!dottedSegment && !isAtEnd() && advanceIf<Token.Comma>())

    if (!dottedSegment) require<Token.RightBracket>("$segmentType segment")

    return selectors.toList()
  }

  private suspend fun State.selector(dottedSegment: Boolean): Selector =
    when (val t = advance()) {
      is Token.Star -> Selector.Wildcard

      is Token.Str -> {
        if (dottedSegment) {
          illegalSelector(
            t,
            "Quoted name selectors are only allowed in bracketed segments",
          )
        }
        Selector.Name(unescape(t))
      }

      is Token.Identifier -> {
        if (!dottedSegment) {
          illegalSelector(
            t,
            "Unquoted name selectors are only allowed in dotted segments",
          )
        }
        Selector.Name(t.value)
      }

      is Token.Num -> {
        if (dottedSegment) {
          illegalSelector(
            t,
            "Index and slice selectors are only allowed in bracketed segments",
          )
        }

        val start = checkInt(t)
        val whitespace = takeIf<Token.Whitespace>()

        if (advanceIf<Token.Colon>()) {
          skipWhiteSpace()

          // Slice Selector
          val end = takeIf<Token.Num>()?.let { checkInt(it) }
          skipWhiteSpace()

          val step =
            if (advanceIf<Token.Colon>()) {
              skipWhiteSpace()
              takeIf<Token.Num>()?.let { checkInt(it) }
            } else {
              null
            }

          Selector.Slice(start, end, step)
        } else {
          if (whitespace != null) raise(JsonPathError.UnexpectedToken(whitespace, "index selector"))
          Selector.Index(start)
        }
      }

      is Token.Colon -> {
        if (dottedSegment) {
          illegalSelector(
            t,
            "Slice selectors are only allowed in bracketed segments",
          )
        }

        skipWhiteSpace()

        // Slice Selector without explicit start
        val end = takeIf<Token.Num>()?.let { checkInt(it) }

        skipWhiteSpace()

        val step = if (advanceIf<Token.Colon>()) takeIf<Token.Num>()?.let { checkInt(it) } else null

        skipWhiteSpace()

        Selector.Slice(null, end, step)
      }

      is Token.QuestionMark -> {
        if (dottedSegment) {
          illegalSelector(
            t,
            "Filter selectors are only allowed in bracketed segments",
          )
        }

        skipWhiteSpace()

        // Filter Selector
        Selector.Filter(filterExpr())
      }

      else -> unexpectedToken(t, "selector")
    }

  private fun State.checkInt(number: Token.Num): Int {
    ensure(number.absolute is Int) {
      JsonPathError.IndicesMustBeIntegers(number.absolute, number.line, number.column)
    }
    ensure(!number.negative || number.absolute != 0) {
      JsonPathError.IllegalCharacter(
        '-',
        number.line,
        number.column,
        "An index of -0 is not allowed",
      )
    }
    return number.intValue
  }

  private suspend fun State.queryExpr(): QueryExpression =
    when (val t = advance()) {
      is Token.At -> QueryExpression.Relative(t, segments())
      is Token.Dollar -> QueryExpression.Absolute(t, segments())
      else -> unexpectedToken(t, "query expression")
    }

  private suspend inline fun <C : ComparableExpression> State.functionExpr(
    identifier: Token.Identifier,
    parse: State.() -> C,
    constructor: (Token, C) -> ComparableExpression,
    requireSingular: Boolean = true,
  ): ComparableExpression {
    require<Token.LeftParen>("${identifier.value} function expression")
    skipWhiteSpace()
    val expr = constructor(identifier, parse().also { if (requireSingular) checkSingular(it) })
    skipWhiteSpace()
    require<Token.RightParen>("${identifier.value} function expression")

    return expr
  }

  private suspend fun State.functionExpr(identifier: Token.Identifier): ComparableExpression =
    when (val name = identifier.value) {
      "length" -> functionExpr(identifier, { comparable() }, FunctionExpression::Length)
      "count" ->
        functionExpr(identifier, {
          queryExpr()
        }, FunctionExpression::Count, requireSingular = false)
      "match", "search" -> {
        require<Token.LeftParen>("$name function expression")
        skipWhiteSpace()
        val firstArg = checkSingular(comparable())

        skipWhiteSpace()
        require<Token.Comma>("$name function expression")
        skipWhiteSpace()

        val secondArg = checkSingular(comparable())
        skipWhiteSpace()
        require<Token.RightParen>("$name function expression")

        FunctionExpression.Match(identifier, firstArg, secondArg, name == "match")
      }

      "value" ->
        functionExpr(identifier, {
          queryExpr()
        }, FunctionExpression::Value, requireSingular = false)
      else -> raise(JsonPathError.UnknownFunction(name, identifier.line, identifier.column))
    }

  private suspend fun State.comparable(): ComparableExpression =
    when (val t = advance()) {
      is Token.Num -> ComparableExpression.Literal(t, t.value)
      is Token.Str -> ComparableExpression.Literal(t, unescape(t))
      is Token.Identifier ->
        when (t.value) {
          "true" -> ComparableExpression.Literal(t, true)
          "false" -> ComparableExpression.Literal(t, false)
          "null" -> ComparableExpression.Literal(t, null)
          else -> functionExpr(t)
        }

      is Token.At -> QueryExpression.Relative(t, segments())
      is Token.Dollar -> QueryExpression.Absolute(t, segments())
      else -> unexpectedToken(t, "comparable expression")
    }

  private suspend fun State.groupExpr(): FilterExpression {
    require<Token.LeftParen>("parenthesized expression")
    skipWhiteSpace()
    val expr = filterExpr()
    skipWhiteSpace()
    require<Token.RightParen>("parenthesized expression")

    return expr
  }

  private suspend fun State.notExpr(): FilterExpression {
    require<Token.Bang>("not expression")
    skipWhiteSpace()
    return FilterExpression.Not(basicLogicalExpr())
  }

  private suspend fun State.basicLogicalExpr(): FilterExpression {
    if (check<Token.Bang>()) {
      return notExpr()
    } else if (check<Token.LeftParen>()) {
      return groupExpr()
    }

    val lhs = comparable()
    skipWhiteSpace()
    val op = takeIf<Token.ComparisonOperator>()

    if (op != null) {
      if (lhs is FunctionExpression.Match) {
        raise(
          JsonPathError.UnexpectedToken(
            op,
            "filter expression with match function",
          ),
        )
      }

      skipWhiteSpace()
      val rhs = comparable()

      return FilterExpression.Comparison(
        op.operator,
        checkSingular(lhs),
        checkSingular(rhs),
      )
    }

    return when (lhs) {
      is FunctionExpression.Match -> lhs
      is NodeListExpression -> FilterExpression.Existence(lhs)
      is ComparableExpression.Literal ->
        raise(
          JsonPathError.UnexpectedToken(lhs.token, "basic logical expression"),
        )
      is FunctionExpression ->
        raise(
          JsonPathError.UnexpectedToken(lhs.token, "basic logical expression"),
        )
    }
  }

  private fun Raise<JsonPathError.MustBeSingularQuery>.checkSingular(
    expr: ComparableExpression,
  ): ComparableExpression =
    expr.apply {
      if (this is QueryExpression &&
        !isSingular
      ) {
        raise(JsonPathError.MustBeSingularQuery(token.line, token.column))
      }
    }

  private suspend fun State.and(): FilterExpression {
    var expr = basicLogicalExpr()
    skipWhiteSpace()

    while (!isAtEnd() && advanceIf<Token.AndAnd>()) {
      skipWhiteSpace()
      val right = basicLogicalExpr()

      expr =
        when (expr) {
          is FilterExpression.And -> FilterExpression.And(expr.operands + right)
          else -> FilterExpression.And(nonEmptyListOf(expr, right))
        }
      skipWhiteSpace()
    }

    return expr
  }

  private suspend fun State.or(): FilterExpression {
    var expr = and()
    skipWhiteSpace()

    while (!isAtEnd() && advanceIf<Token.PipePipe>()) {
      skipWhiteSpace()

      val right = and()

      expr =
        when (expr) {
          is FilterExpression.Or -> FilterExpression.Or(expr.operands + right)
          else -> FilterExpression.Or(nonEmptyListOf(expr, right))
        }

      skipWhiteSpace()
    }

    return expr
  }

  private suspend fun State.skipWhiteSpace() {
    while (advanceIf<Token.Whitespace>()) {
      // Drop all whitespace tokens
    }
  }

  private suspend fun State.peek(): Token =
    current ?: run {
      val first = receiveToken()!!
      current = first
      first
    }

  private suspend fun State.advance(): Token =
    peek().also {
      current = receiveToken()
    }

  private suspend inline fun <reified T : Token> State.advanceIf(): Boolean {
    if (check<T>()) {
      advance()
      return true
    }

    return false
  }

  private suspend inline fun <reified T : Token> State.takeIf(): T? {
    if (check<T>()) {
      return advance() as T
    }

    return null
  }

  private suspend inline fun <reified T : Token> State.require(parsing: String): T =
    takeIf<T>() ?: unexpectedToken(peek(), parsing)

  private suspend inline fun <reified T : Token> State.check(): Boolean = peek() is T

  private suspend fun State.isAtEnd(): Boolean = check<Token.Eof>()

  private class State(
    tokens: ReceiveChannel<Token>,
    raise: Raise<JsonPathError>,
    var current: Token? = null,
  ) : Raise<JsonPathError> by raise,
    ReceiveChannel<Token> by tokens {
    suspend fun receiveToken(): Token? =
      receiveCatching().let {
        when {
          it.isClosed -> it.exceptionOrNull()?.let { t -> raise(JsonPathError(t)) }
          it.isSuccess -> it.getOrThrow()
          else -> error("Unexpected Failed channel result")
        }
      }
  }

  private fun State.unexpectedToken(
    token: Token,
    parsing: String,
  ): Nothing {
    raise(JsonPathError.UnexpectedToken(token, parsing))
  }

  private fun State.illegalSelector(
    token: Token,
    reason: String,
  ): Nothing = raise(JsonPathError.IllegalSelector(token.line, token.column, reason))
}
