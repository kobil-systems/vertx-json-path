package com.kobil.vertx.jsonpath

/**
 * The base interface for all expressions that may occur as operands of a comparison.
 *
 * It includes methods to build comparison expressions and some function expressions. One simple
 * subclass is a [Literal] expression.
 *
 * @see FilterExpression.Comparison
 * @see FunctionExpression
 * @see QueryExpression
 */
sealed interface ComparableExpression {
  /**
   * Construct a [FilterExpression.Comparison] checking for equality of this expression and [other]
   *
   * @param other the right hand side of the comparison
   * @return a '==' comparison
   */
  infix fun eq(other: ComparableExpression): FilterExpression = isEqualTo(other)

  /**
   * Construct a [FilterExpression.Comparison] checking for equality of this expression and [other]
   *
   * @param other the right hand side of the comparison
   * @return a '==' comparison
   */
  fun isEqualTo(other: ComparableExpression): FilterExpression =
    FilterExpression.Comparison.eq(this, other)

  /**
   * Construct a [FilterExpression.Comparison] checking that this expression is not equal to [other]
   *
   * @param other the right hand side of the comparison
   * @return a '!=' comparison
   */
  infix fun neq(other: ComparableExpression): FilterExpression = isNotEqualTo(other)

  /**
   * Construct a [FilterExpression.Comparison] checking that this expression is not equal to [other]
   *
   * @param other the right hand side of the comparison
   * @return a '!=' comparison
   */
  fun isNotEqualTo(other: ComparableExpression): FilterExpression =
    FilterExpression.Comparison.neq(this, other)

  /**
   * Construct a [FilterExpression.Comparison] checking that this expression is greater than [other]
   *
   * @param other the right hand side of the comparison
   * @return a '>' comparison
   */
  infix fun gt(other: ComparableExpression): FilterExpression = isGreaterThan(other)

  /**
   * Construct a [FilterExpression.Comparison] checking that this expression is greater than [other]
   *
   * @param other the right hand side of the comparison
   * @return a '>' comparison
   */
  fun isGreaterThan(other: ComparableExpression): FilterExpression =
    FilterExpression.Comparison.greaterThan(this, other)

  /**
   * Construct a [FilterExpression.Comparison] checking that this expression is greater than or
   * equal to [other]
   *
   * @param other the right hand side of the comparison
   * @return a '>=' comparison
   */
  infix fun ge(other: ComparableExpression): FilterExpression = isGreaterOrEqual(other)

  /**
   * Construct a [FilterExpression.Comparison] checking that this expression is greater than or
   * equal to [other]
   *
   * @param other the right hand side of the comparison
   * @return a '>=' comparison
   */
  fun isGreaterOrEqual(other: ComparableExpression): FilterExpression =
    FilterExpression.Comparison.greaterOrEqual(this, other)

  /**
   * Construct a [FilterExpression.Comparison] checking that this expression is less than [other]
   *
   * @param other the right hand side of the comparison
   * @return a '<' comparison
   */
  infix fun lt(other: ComparableExpression): FilterExpression = isLessThan(other)

  /**
   * Construct a [FilterExpression.Comparison] checking that this expression is less than [other]
   *
   * @param other the right hand side of the comparison
   * @return a '<' comparison
   */
  fun isLessThan(other: ComparableExpression): FilterExpression =
    FilterExpression.Comparison.lessThan(this, other)

  /**
   * Construct a [FilterExpression.Comparison] checking that this expression is less than or equal
   * to [other]
   *
   * @param other the right hand side of the comparison
   * @return a '<=' comparison
   */
  infix fun le(other: ComparableExpression): FilterExpression = isLessOrEqual(other)

  /**
   * Construct a [FilterExpression.Comparison] checking that this expression is less than or equal
   * to [other]
   *
   * @param other the right hand side of the comparison
   * @return a '<=' comparison
   */
  fun isLessOrEqual(other: ComparableExpression): FilterExpression =
    FilterExpression.Comparison.lessOrEqual(this, other)

  /**
   * Construct a [FunctionExpression.Length] that calculates the length of a string or size of a
   * [io.vertx.core.json.JsonObject] or [io.vertx.core.json.JsonArray]
   *
   * @return a [FunctionExpression.Length] call taking this expression as the argument
   */
  fun length(): ComparableExpression = FunctionExpression.Length(this)

  /**
   * Construct a `match` function call that matches this expression to some regex [pattern]. Both
   * operands may either be strings or singular query expressions (relative or absolute). The
   * `match` function always matches the entire subject string.
   *
   * @see FilterExpression.Match
   * @param pattern the expression specifying the pattern to match
   * @return a `match` function expression using this as the subject and [pattern] as the pattern
   */
  fun match(pattern: ComparableExpression): FilterExpression =
    FilterExpression.Match(this, pattern, true)

  /**
   * Construct a `search` function call that matches this expression to some regex [pattern]. Both
   * operands may either be strings or singular query expressions (relative or absolute). The
   * `search` function looks for a matching substring inside the subject string.
   *
   * @see FilterExpression.Match
   * @param pattern the expression specifying the pattern to match
   * @return a `search` function expression using this as the subject and [pattern] as the pattern
   */
  fun search(pattern: ComparableExpression): FilterExpression =
    FilterExpression.Match(this, pattern, false)

  /**
   * A literal expression, which may be an integer, floating point number, string,
   * boolean or `null`.
   */
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
}
