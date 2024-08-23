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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlin.coroutines.coroutineContext

internal suspend fun Flow<Token>.parseJsonPathQuery(): Either<JsonPathError, JsonPath> =
  either {
    val ch = collectIntoChannel()

    try {
      ParserState(ch, this).run {
        require<Token.Dollar>("JSON path query")
        JsonPath(segments())
      }
    } finally {
      ch.cancel()
    }
  }

internal suspend fun Flow<Token>.parseJsonPathFilter(): Either<JsonPathError, FilterExpression> =
  either {
    val ch = collectIntoChannel()

    try {
      ParserState(ch, this).filterExpr()
    } finally {
      ch.cancel()
    }
  }

private suspend fun Flow<Token>.collectIntoChannel(): ReceiveChannel<Token> =
  Channel<Token>().also { ch ->
    catch { ch.close(it) }
      .onCompletion { ch.close(it) }
      .onEach(ch::send)
      .launchIn(CoroutineScope(coroutineContext))
  }

private suspend fun ParserState.filterExpr() = or()

private suspend fun ParserState.segments(): List<Segment> =
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

private suspend fun ParserState.childSegment(dottedSegment: Boolean): Segment =
  Segment.ChildSegment(selectors(dottedSegment, "child"))

private suspend fun ParserState.descendantSegment(): Segment {
  val dottedSegment = !advanceIf<Token.LeftBracket>()
  return Segment.DescendantSegment(selectors(dottedSegment, "descendant"))
}

private suspend fun ParserState.selectors(
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

private suspend fun ParserState.selector(dottedSegment: Boolean): Selector =
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

private suspend fun ParserState.queryExpr(): QueryExpression =
  when (val t = advance()) {
    is Token.At -> QueryExpression.Relative(segments(), t)
    is Token.Dollar -> QueryExpression.Absolute(segments(), t)
    else -> unexpectedToken(t, "query expression")
  }

private suspend inline fun <C : ComparableExpression> ParserState.functionExpr(
  identifier: Token.Identifier,
  parse: ParserState.() -> C,
  constructor: (C, Token?) -> ComparableExpression,
  requireSingular: Boolean = true,
): ComparableExpression {
  require<Token.LeftParen>("${identifier.value} function expression")
  skipWhiteSpace()
  val expr = constructor(parse().also { if (requireSingular) checkSingular(it) }, identifier)
  skipWhiteSpace()
  require<Token.RightParen>("${identifier.value} function expression")

  return expr
}

private suspend fun ParserState.functionExpr(identifier: Token.Identifier): ComparableExpression =
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

private suspend fun ParserState.comparable(): ComparableExpression =
  when (val t = advance()) {
    is Token.Integer -> ComparableExpression.Literal(t.value, t)
    is Token.Decimal -> ComparableExpression.Literal(t.value, t)
    is Token.Str -> ComparableExpression.Literal(unescape(t), t)
    is Token.Identifier ->
      when (t.value) {
        "true" -> ComparableExpression.Literal(true, t)
        "false" -> ComparableExpression.Literal(false, t)
        "null" -> ComparableExpression.Literal(null, t)
        else -> functionExpr(t)
      }

    is Token.At -> QueryExpression.Relative(segments(), t)
    is Token.Dollar -> QueryExpression.Absolute(segments(), t)
    else -> unexpectedToken(t, "comparable expression")
  }

private suspend fun ParserState.groupExpr(): FilterExpression {
  require<Token.LeftParen>("parenthesized expression")
  skipWhiteSpace()
  val expr = filterExpr()
  skipWhiteSpace()
  require<Token.RightParen>("parenthesized expression")

  return expr
}

private suspend fun ParserState.notExpr(): FilterExpression {
  require<Token.Bang>("not expression")
  skipWhiteSpace()
  return FilterExpression.Not(basicLogicalExpr())
}

private suspend fun ParserState.matchOrSearchFunction(): FilterExpression {
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

  return FilterExpression.Match(firstArg, secondArg, name == "match", identifier)
}

private suspend fun ParserState.basicLogicalExpr(): FilterExpression {
  if (check<Token.Bang>()) {
    return notExpr()
  } else if (check<Token.LeftParen>()) {
    return groupExpr()
  }

  val matchOrSearch = (current as? Token.Identifier)?.takeIf { it.value in matchOrSearch }

  if (matchOrSearch != null) return matchOrSearchFunction()

  val lhs = comparable()
  skipWhiteSpace()
  val op = takeIf<Token.ComparisonOperator>()

  if (op != null) {
    skipWhiteSpace()
    val rhs = comparable()

    return FilterExpression.Comparison(
      op.operator,
      checkSingular(lhs),
      checkSingular(rhs),
    )
  }

  return when (lhs) {
    is NodeListExpression -> FilterExpression.Existence(lhs)
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

private fun Raise<JsonPathError.MustBeSingularQuery>.checkSingular(
  expr: ComparableExpression,
): ComparableExpression =
  expr.apply {
    if (this is QueryExpression &&
      !isSingular
    ) {
      raise(JsonPathError.MustBeSingularQuery(token!!.line, token!!.column))
    }
  }

private suspend fun ParserState.and(): FilterExpression {
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

private suspend fun ParserState.or(): FilterExpression {
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

private suspend fun ParserState.skipWhiteSpace() {
  while (advanceIf<Token.Whitespace>()) {
    // Drop all whitespace tokens
  }
}

private suspend fun ParserState.peek(): Token =
  current ?: run {
    val first = receiveToken()!!
    current = first
    first
  }

private suspend fun ParserState.advance(): Token =
  peek().also {
    current = receiveToken()
  }

private suspend inline fun <reified T : Token> ParserState.advanceIf(): Boolean {
  if (check<T>()) {
    advance()
    return true
  }

  return false
}

private suspend inline fun <reified T> ParserState.takeIf(): T? {
  if (check<T>()) {
    return advance() as T
  }

  return null
}

private suspend inline fun <reified T : Token> ParserState.require(parsing: String): T =
  takeIf<T>() ?: unexpectedToken(peek(), parsing)

private suspend inline fun <reified T> ParserState.check(): Boolean = peek() is T

private suspend fun ParserState.isAtEnd(): Boolean = check<Token.Eof>()

private class ParserState(
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
