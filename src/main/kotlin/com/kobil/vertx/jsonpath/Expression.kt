package com.kobil.vertx.jsonpath

import arrow.core.Either
import arrow.core.NonEmptyList
import com.kobil.vertx.jsonpath.JsonNode.Companion.rootNode
import com.kobil.vertx.jsonpath.compiler.JsonPathCompiler.compileJsonPathFilter
import com.kobil.vertx.jsonpath.compiler.Token
import com.kobil.vertx.jsonpath.error.JsonPathError
import com.kobil.vertx.jsonpath.interpreter.match
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

sealed interface FilterExpression {
  fun match(obj: JsonObject): Boolean = match(obj.rootNode)

  fun match(arr: JsonArray): Boolean = match(arr.rootNode)

  data class And(
    val operands: NonEmptyList<FilterExpression>,
  ) : FilterExpression

  data class Or(
    val operands: NonEmptyList<FilterExpression>,
  ) : FilterExpression

  data class Not(
    val operand: FilterExpression,
  ) : FilterExpression

  data class Comparison(
    val op: Op,
    val lhs: ComparableExpression,
    val rhs: ComparableExpression,
  ) : FilterExpression {
    enum class Op {
      EQ,
      NOT_EQ,
      LESS,
      LESS_EQ,
      GREATER,
      GREATER_EQ,
    }
  }

  data class Existence(
    val query: NodeListExpression,
  ) : FilterExpression

  data class Match(
    val subject: ComparableExpression,
    val pattern: ComparableExpression,
    val matchEntire: Boolean,
    val token: Token? = null,
  ) : FilterExpression

  companion object {
    fun compile(filterExpression: String): Either<JsonPathError, FilterExpression> =
      compileJsonPathFilter(filterExpression)
  }
}

sealed interface ComparableExpression {
  data class Literal(
    val value: Any?,
    val token: Token? = null,
  ) : ComparableExpression
}

sealed interface NodeListExpression : ComparableExpression

sealed interface FunctionExpression : ComparableExpression {
  val token: Token?

  data class Length(
    val arg: ComparableExpression,
    override val token: Token? = null,
  ) : FunctionExpression

  data class Count(
    val arg: QueryExpression,
    override val token: Token? = null,
  ) : FunctionExpression

  data class Value(
    val arg: QueryExpression,
    override val token: Token? = null,
  ) : FunctionExpression
}

sealed interface QueryExpression : NodeListExpression {
  val token: Token?
  val segments: List<Segment>
  val isSingular: Boolean
    get() = segments.all { it.isSingular }

  data class Relative(
    override val segments: List<Segment> = listOf(),
    override val token: Token? = null,
  ) : QueryExpression

  data class Absolute(
    override val segments: List<Segment> = listOf(),
    override val token: Token? = null,
  ) : QueryExpression
}
