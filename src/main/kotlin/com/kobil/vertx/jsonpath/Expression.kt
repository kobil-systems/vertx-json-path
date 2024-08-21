package com.kobil.vertx.jsonpath

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.None
import arrow.core.Option
import arrow.core.left
import arrow.core.right
import arrow.core.some
import com.kobil.vertx.jsonpath.compiler.JsonPathCompiler.compileJsonPathFilter
import com.kobil.vertx.jsonpath.compiler.JsonPathCompiler.compileJsonPathQuery
import com.kobil.vertx.jsonpath.compiler.Token
import com.kobil.vertx.jsonpath.error.JsonPathError
import com.kobil.vertx.jsonpath.error.MultipleResults
import io.vertx.core.Vertx

sealed interface FilterExpression {
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

  companion object {
    suspend fun compile(
      vertx: Vertx,
      filterExpression: String,
    ): Either<JsonPathError, FilterExpression> = vertx.compileJsonPathFilter(filterExpression)
  }
}

sealed interface ComparableExpression {
  data class Literal(
    val token: Token,
    val value: Any?,
  ) : ComparableExpression
}

sealed interface NodeListExpression : ComparableExpression

sealed interface FunctionExpression : ComparableExpression {
  val token: Token

  data class Length(
    override val token: Token,
    val arg: ComparableExpression,
  ) : FunctionExpression

  data class Count(
    override val token: Token,
    val arg: QueryExpression,
  ) : FunctionExpression

  data class Match(
    override val token: Token,
    val subject: ComparableExpression,
    val pattern: ComparableExpression,
    val full: Boolean = true,
  ) : FunctionExpression,
    FilterExpression

  data class Value(
    override val token: Token,
    val arg: QueryExpression,
  ) : FunctionExpression
}

sealed interface QueryExpression : NodeListExpression {
  val token: Token
  val segments: List<Segment>
  val isSingular: Boolean
    get() = segments.all { it.isSingular }

  data class Relative(
    override val token: Token,
    override val segments: List<Segment>,
  ) : QueryExpression

  data class Absolute(
    override val token: Token,
    override val segments: List<Segment>,
  ) : QueryExpression
}
