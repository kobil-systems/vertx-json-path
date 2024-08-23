package com.kobil.vertx.jsonpath.compiler

import com.kobil.vertx.jsonpath.FilterExpression

sealed interface Token {
  val line: UInt
  val column: UInt
  val name: String
    get() = this::class.simpleName!!

  sealed interface ComparisonOperator {
    val operator: FilterExpression.Comparison.Op
  }

  sealed interface Value<T> {
    val value: T
  }

  data class Whitespace(
    override val line: UInt,
    override val column: UInt,
  ) : Token

  data class Eof(
    override val line: UInt,
    override val column: UInt,
  ) : Token

  data class Dollar(
    override val line: UInt,
    override val column: UInt,
  ) : Token

  data class At(
    override val line: UInt,
    override val column: UInt,
  ) : Token

  data class Comma(
    override val line: UInt,
    override val column: UInt,
  ) : Token

  data class Star(
    override val line: UInt,
    override val column: UInt,
  ) : Token

  data class Colon(
    override val line: UInt,
    override val column: UInt,
  ) : Token

  data class QuestionMark(
    override val line: UInt,
    override val column: UInt,
  ) : Token

  data class LeftBracket(
    override val line: UInt,
    override val column: UInt,
  ) : Token

  data class RightBracket(
    override val line: UInt,
    override val column: UInt,
  ) : Token

  data class LeftParen(
    override val line: UInt,
    override val column: UInt,
  ) : Token

  data class RightParen(
    override val line: UInt,
    override val column: UInt,
  ) : Token

  data class Dot(
    override val line: UInt,
    override val column: UInt,
  ) : Token

  data class DotDot(
    override val line: UInt,
    override val column: UInt,
  ) : Token

  data class EqEq(
    override val line: UInt,
    override val column: UInt,
  ) : Token,
    ComparisonOperator {
    override val operator: FilterExpression.Comparison.Op = FilterExpression.Comparison.Op.EQ
  }

  data class Bang(
    override val line: UInt,
    override val column: UInt,
  ) : Token

  data class BangEq(
    override val line: UInt,
    override val column: UInt,
  ) : Token,
    ComparisonOperator {
    override val operator: FilterExpression.Comparison.Op = FilterExpression.Comparison.Op.NOT_EQ
  }

  data class Less(
    override val line: UInt,
    override val column: UInt,
  ) : Token,
    ComparisonOperator {
    override val operator: FilterExpression.Comparison.Op = FilterExpression.Comparison.Op.LESS
  }

  data class LessEq(
    override val line: UInt,
    override val column: UInt,
  ) : Token,
    ComparisonOperator {
    override val operator: FilterExpression.Comparison.Op = FilterExpression.Comparison.Op.LESS_EQ
  }

  data class Greater(
    override val line: UInt,
    override val column: UInt,
  ) : Token,
    ComparisonOperator {
    override val operator: FilterExpression.Comparison.Op = FilterExpression.Comparison.Op.GREATER
  }

  data class GreaterEq(
    override val line: UInt,
    override val column: UInt,
  ) : Token,
    ComparisonOperator {
    override val operator: FilterExpression.Comparison.Op =
      FilterExpression.Comparison.Op.GREATER_EQ
  }

  data class AndAnd(
    override val line: UInt,
    override val column: UInt,
  ) : Token

  data class PipePipe(
    override val line: UInt,
    override val column: UInt,
  ) : Token

  data class Str(
    override val line: UInt,
    override val column: UInt,
    override val value: String,
  ) : Token,
    Value<String>

  data class Integer(
    override val line: UInt,
    override val column: UInt,
    val negative: Boolean,
    override val value: Int,
  ) : Token,
    Value<Int>

  data class Decimal(
    override val line: UInt,
    override val column: UInt,
    override val value: Double,
  ) : Token,
    Value<Double>

  data class Identifier(
    override val line: UInt,
    override val column: UInt,
    override val value: String,
  ) : Token,
    Value<String>
}
