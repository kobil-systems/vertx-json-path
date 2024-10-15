package com.kobil.vertx.jsonpath

/**
 * An expression representing a JSON Path Function extension. It may appear as an operand in a
 * [FilterExpression.Comparison].
 */
sealed interface FunctionExpression : ComparableExpression {
  /**
   * The `length` function extension, returning the length of a string argument, the number of
   * elements in a JSON array, or the number of entries in a JSON object.
   *
   * @param arg the argument to the function, e.g. a [ComparableExpression.Literal]
   *   or a [QueryExpression]
   */
  data class Length(
    val arg: ComparableExpression,
  ) : FunctionExpression {
    /**
     * Returns the JSON path representation of this function expression.
     */
    override fun toString(): String = "length($arg)"
  }

  /**
   * The `count` function extension, returning the length of the node list returned by the query.
   *
   * @param arg the argument to the function, a [NodeListExpression]
   * @see NodeListExpression
   * @see QueryExpression
   */
  data class Count(
    val arg: NodeListExpression,
  ) : FunctionExpression {
    /**
     * Returns the JSON path representation of this function expression.
     */
    override fun toString(): String = "count($arg)"
  }

  /**
   * The `value` function extension, returning the single item of the node list resulting from the
   * query. If the query doesn't return exactly one node, the function will result in an error.
   *
   * @param arg the argument to the function, a [NodeListExpression]
   * @see NodeListExpression
   * @see QueryExpression
   */
  data class Value(
    val arg: NodeListExpression,
  ) : FunctionExpression {
    /**
     * Returns the JSON path representation of this function expression.
     */
    override fun toString(): String = "value($arg)"
  }
}
