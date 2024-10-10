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
import com.kobil.vertx.jsonpath.error.JsonPathError

internal fun Sequence<TokenEvent>.parseJsonPathQuery(): Either<JsonPathError, JsonPath> =
  either {
    ParserState(iterator(), this).run {
      require<Token.Dollar>("JSON path query")
      JsonPath(segments())
    }
  }

internal fun Sequence<TokenEvent>.parseJsonPathFilter() =
  either {
    ParserState(iterator(), this).filterExpr()
  }

private fun ParserState.filterExpr() = or()

private fun ParserState.segments(): List<Segment> =
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

private fun ParserState.childSegment(dottedSegment: Boolean): Segment =
  Segment.ChildSegment(selectors(dottedSegment, "child"))

private fun ParserState.descendantSegment(): Segment {
  val dottedSegment = !advanceIf<Token.LeftBracket>()
  return Segment.DescendantSegment(selectors(dottedSegment, "descendant"))
}

private fun ParserState.selectors(
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

private fun ParserState.selector(dottedSegment: Boolean): Selector =
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

    is Token.Integer -> {
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
        val end = takeIf<Token.Integer>()?.let { checkInt(it) }
        skipWhiteSpace()

        val step =
          if (advanceIf<Token.Colon>()) {
            skipWhiteSpace()
            takeIf<Token.Integer>()?.let { checkInt(it) }
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
      val end = takeIf<Token.Integer>()?.let { checkInt(it) }

      skipWhiteSpace()

      val step =
        if (advanceIf<Token.Colon>()) takeIf<Token.Integer>()?.let { checkInt(it) } else null

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

private fun ParserState.checkInt(number: Token.Integer): Int {
  ensure(!number.negative || number.value != 0) {
    JsonPathError.IllegalCharacter(
      '-',
      number.line,
      number.column,
      "An index of -0 is not allowed",
    )
  }
  return number.value
}

private fun ParserState.queryExpr(): Pair<QueryExpression<*>, Token> =
  when (val t = advance()) {
    is Token.At -> QueryExpression.Relative(segments()) to t
    is Token.Dollar -> QueryExpression.Absolute(segments()) to t
    else -> unexpectedToken(t, "query expression")
  }

private inline fun <C : ComparableExpression> ParserState.functionExpr(
  identifier: Token.Identifier,
  parse: ParserState.() -> Pair<C, Token>,
  constructor: (C) -> ComparableExpression,
  requireSingular: Boolean = true,
): ComparableExpression {
  require<Token.LeftParen>("${identifier.value} function expression")
  skipWhiteSpace()
  val expr =
    constructor(parse().let { if (requireSingular) checkSingular(it) else it.first })
  skipWhiteSpace()
  require<Token.RightParen>("${identifier.value} function expression")

  return expr
}

private fun ParserState.functionExpr(identifier: Token.Identifier): ComparableExpression =
  when (val name = identifier.value) {
    "length" -> functionExpr(identifier, { comparable() }, FunctionExpression::Length)
    "count" ->
      functionExpr(identifier, {
        queryExpr()
      }, FunctionExpression::Count, requireSingular = false)

    "value" ->
      functionExpr(identifier, {
        queryExpr()
      }, FunctionExpression::Value, requireSingular = false)

    else -> raise(JsonPathError.UnknownFunction(name, identifier.line, identifier.column))
  }

private fun ParserState.comparable(): Pair<ComparableExpression, Token> =
  when (val t = advance()) {
    is Token.Integer -> ComparableExpression.Literal(t.value) to t
    is Token.Decimal -> ComparableExpression.Literal(t.value) to t
    is Token.Str -> ComparableExpression.Literal(unescape(t)) to t
    is Token.Identifier ->
      when (t.value) {
        "true" -> ComparableExpression.Literal(true)
        "false" -> ComparableExpression.Literal(false)
        "null" -> ComparableExpression.Literal(null)
        else -> functionExpr(t)
      } to t

    is Token.At -> QueryExpression.Relative(segments()) to t
    is Token.Dollar -> QueryExpression.Absolute(segments()) to t
    else -> unexpectedToken(t, "comparable expression")
  }

private fun ParserState.groupExpr(): FilterExpression {
  require<Token.LeftParen>("parenthesized expression")
  skipWhiteSpace()
  val expr = filterExpr()
  skipWhiteSpace()
  require<Token.RightParen>("parenthesized expression")

  return expr
}

private fun ParserState.notExpr(): FilterExpression {
  require<Token.Bang>("not expression")
  skipWhiteSpace()
  return FilterExpression.Not(basicLogicalExpr())
}

private fun ParserState.matchOrSearchFunction(): FilterExpression {
  val identifier = require<Token.Identifier>("basic logical expression")
  val name = identifier.value

  require<Token.LeftParen>("$name function expression")
  skipWhiteSpace()
  val firstArg = checkSingular(comparable())

  skipWhiteSpace()
  require<Token.Comma>("$name function expression")
  skipWhiteSpace()

  val secondArg = checkSingular(comparable())
  skipWhiteSpace()
  require<Token.RightParen>("$name function expression")

  return FilterExpression.Match(firstArg, secondArg, name == "match")
}

private fun ParserState.basicLogicalExpr(): FilterExpression {
  if (check<Token.Bang>()) {
    return notExpr()
  } else if (check<Token.LeftParen>()) {
    return groupExpr()
  }

  val matchOrSearch = (current as? Token.Identifier)?.takeIf { it.value in matchOrSearch }

  if (matchOrSearch != null) return matchOrSearchFunction()

  val (lhs, lhsToken) = comparable()
  skipWhiteSpace()
  val op = takeIf<Token.ComparisonOperator>()

  if (op != null) {
    skipWhiteSpace()
    val (rhs, rhsToken) = comparable()

    return FilterExpression.Comparison(
      op.operator,
      checkSingular(lhs, lhsToken),
      checkSingular(rhs, rhsToken),
    )
  }

  return when (lhs) {
    is NodeListExpression -> FilterExpression.Test(lhs)
    is ComparableExpression.Literal ->
      raise(
        JsonPathError.UnexpectedToken(current!!, "basic logical expression"),
      )

    is FunctionExpression ->
      raise(
        JsonPathError.UnexpectedToken(current!!, "basic logical expression"),
      )
  }
}

private fun <C : ComparableExpression> Raise<JsonPathError.MustBeSingularQuery>.checkSingular(
  expr: Pair<C, Token>,
): C = checkSingular(expr.first, expr.second)

private fun <C : ComparableExpression> Raise<JsonPathError.MustBeSingularQuery>.checkSingular(
  expr: C,
  token: Token,
): C =
  expr.apply {
    if (this is QueryExpression<*> &&
      !isSingular
    ) {
      raise(JsonPathError.MustBeSingularQuery(token.line, token.column))
    }
  }

private fun ParserState.and(): FilterExpression {
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

private fun ParserState.or(): FilterExpression {
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

private fun ParserState.skipWhiteSpace() {
  while (advanceIf<Token.Whitespace>()) {
    // Drop all whitespace tokens
  }
}

private fun ParserState.peek(): Token =
  current ?: run {
    val first = receiveToken()
    current = first
    first
  }

private fun ParserState.advance(): Token =
  peek().also {
    if (!isAtEnd()) current = receiveToken()
  }

private inline fun <reified T : Token> ParserState.advanceIf(): Boolean {
  if (check<T>()) {
    advance()
    return true
  }

  return false
}

private inline fun <reified T> ParserState.takeIf(): T? {
  if (check<T>()) {
    return advance() as T
  }

  return null
}

private inline fun <reified T : Token> ParserState.require(parsing: String): T =
  takeIf<T>() ?: unexpectedToken(peek(), parsing)

private inline fun <reified T> ParserState.check(): Boolean = peek() is T

private fun ParserState.isAtEnd(): Boolean = check<Token.Eof>()

private class ParserState(
  tokens: Iterator<Either<JsonPathError, Token>>,
  raise: Raise<JsonPathError>,
  var current: Token? = null,
) : Raise<JsonPathError> by raise,
  Iterator<Either<JsonPathError, Token>> by tokens {
  fun receiveToken(): Token = next().bind()
}

private fun ParserState.unexpectedToken(
  token: Token,
  parsing: String,
): Nothing {
  raise(JsonPathError.UnexpectedToken(token, parsing))
}

private fun ParserState.illegalSelector(
  token: Token,
  reason: String,
): Nothing = raise(JsonPathError.IllegalSelector(token.line, token.column, reason))

private val matchOrSearch = setOf("match", "search")
