package com.kobil.vertx.jsonpath

sealed interface ComparableExpression {
  infix fun eq(other: ComparableExpression): FilterExpression = isEqualTo(other)

  fun isEqualTo(other: ComparableExpression): FilterExpression =
    FilterExpression.Comparison.eq(this, other)

  infix fun neq(other: ComparableExpression): FilterExpression = isNotEqualTo(other)

  fun isNotEqualTo(other: ComparableExpression): FilterExpression =
    FilterExpression.Comparison.neq(this, other)

  infix fun gt(other: ComparableExpression): FilterExpression = isGreaterThan(other)

  fun isGreaterThan(other: ComparableExpression): FilterExpression =
    FilterExpression.Comparison.greaterThan(this, other)

  infix fun ge(other: ComparableExpression): FilterExpression = isGreaterOrEqual(other)

  fun isGreaterOrEqual(other: ComparableExpression): FilterExpression =
    FilterExpression.Comparison.greaterOrEqual(this, other)

  infix fun lt(other: ComparableExpression): FilterExpression = isLessThan(other)

  fun isLessThan(other: ComparableExpression): FilterExpression =
    FilterExpression.Comparison.lessThan(this, other)

  infix fun le(other: ComparableExpression): FilterExpression = isLessOrEqual(other)

  fun isLessOrEqual(other: ComparableExpression): FilterExpression =
    FilterExpression.Comparison.lessOrEqual(this, other)

  fun length(): ComparableExpression = FunctionExpression.Length(this)

  fun match(pattern: ComparableExpression): FilterExpression =
    FilterExpression.Match(this, pattern, true)

  fun search(pattern: ComparableExpression): FilterExpression =
    FilterExpression.Match(this, pattern, false)

  data class Literal(
    val value: Any?,
  ) : ComparableExpression {
    override fun toString(): String =
      when (value) {
        null -> "null"
        is String -> "\"$value\""
        else -> value.toString()
      }
  }

  companion object {
    @JvmStatic
    fun literal(value: Int): Literal = Literal(value)

    @JvmStatic
    fun literal(value: Double): Literal = Literal(value)

    @JvmStatic
    fun literal(value: String): Literal = Literal(value)

    @JvmStatic
    fun literal(value: Boolean): Literal = Literal(value)

    @JvmStatic
    fun literalNull(): Literal = Literal(null)
  }
}
