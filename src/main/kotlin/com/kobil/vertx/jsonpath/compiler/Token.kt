package com.kobil.vertx.jsonpath.compiler

import com.kobil.vertx.jsonpath.FilterExpression

sealed interface Token {
  val line: Int
  val column: Int
  val name: String
    get() = this::class.simpleName!!

  sealed interface ComparisonOperator : Token {
    val operator: FilterExpression.Comparison.Op
  }

  sealed interface StringValueToken : Token {
    val value: String
  }

  data class Whitespace(
    override val line: Int,
    override val column: Int,
  ) : Token

  data class Eof(
    override val line: Int,
    override val column: Int,
  ) : Token

  data class Dollar(
    override val line: Int,
    override val column: Int,
  ) : Token

  data class At(
    override val line: Int,
    override val column: Int,
  ) : Token

  data class Comma(
    override val line: Int,
    override val column: Int,
  ) : Token

  data class Star(
    override val line: Int,
    override val column: Int,
  ) : Token

  data class Colon(
    override val line: Int,
    override val column: Int,
  ) : Token

  data class QuestionMark(
    override val line: Int,
    override val column: Int,
  ) : Token

  data class LeftBracket(
    override val line: Int,
    override val column: Int,
  ) : Token

  data class RightBracket(
    override val line: Int,
    override val column: Int,
  ) : Token

  data class LeftParen(
    override val line: Int,
    override val column: Int,
  ) : Token

  data class RightParen(
    override val line: Int,
    override val column: Int,
  ) : Token

  data class Dot(
    override val line: Int,
    override val column: Int,
  ) : Token

  data class DotDot(
    override val line: Int,
    override val column: Int,
  ) : Token

  data class EqEq(
    override val line: Int,
    override val column: Int,
  ) : ComparisonOperator {
    override val operator: FilterExpression.Comparison.Op = FilterExpression.Comparison.Op.EQ
  }

  data class Bang(
    override val line: Int,
    override val column: Int,
  ) : Token

  data class BangEq(
    override val line: Int,
    override val column: Int,
  ) : ComparisonOperator {
    override val operator: FilterExpression.Comparison.Op = FilterExpression.Comparison.Op.NOT_EQ
  }

  data class Less(
    override val line: Int,
    override val column: Int,
  ) : ComparisonOperator {
    override val operator: FilterExpression.Comparison.Op = FilterExpression.Comparison.Op.LESS
  }

  data class LessEq(
    override val line: Int,
    override val column: Int,
  ) : ComparisonOperator {
    override val operator: FilterExpression.Comparison.Op = FilterExpression.Comparison.Op.LESS_EQ
  }

  data class Greater(
    override val line: Int,
    override val column: Int,
  ) : ComparisonOperator {
    override val operator: FilterExpression.Comparison.Op = FilterExpression.Comparison.Op.GREATER
  }

  data class GreaterEq(
    override val line: Int,
    override val column: Int,
  ) : ComparisonOperator {
    override val operator: FilterExpression.Comparison.Op =
      FilterExpression.Comparison.Op.GREATER_EQ
  }

  data class AndAnd(
    override val line: Int,
    override val column: Int,
  ) : Token

  data class PipePipe(
    override val line: Int,
    override val column: Int,
  ) : Token

  data class Str(
    override val line: Int,
    override val column: Int,
    override val value: String,
  ) : StringValueToken

  data class Num(
    override val line: Int,
    override val column: Int,
    val negative: Boolean,
    val absolute: Number,
  ) : Token {
    val intValue: Int
      get() = if (negative) -(absolute as Int) else absolute as Int
    val value: Number
      get() =
        when (absolute) {
          is Double -> if (negative) -absolute else absolute
          is Int -> if (negative) -absolute else absolute
          else -> error("unreachable")
        }
  }

  data class Identifier(
    override val line: Int,
    override val column: Int,
    override val value: String,
  ) : StringValueToken
}
