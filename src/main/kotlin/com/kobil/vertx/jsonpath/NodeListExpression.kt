package com.kobil.vertx.jsonpath

/**
 * An expression that results in a node list. This may be a query expression or a function
 * expression that returns a node list.
 */
sealed interface NodeListExpression : ComparableExpression {
  /**
   * Returns a test expression, testing for existence of any nodes matching
   * this [NodeListExpression].
   *
   * @return a test expression checking that the returned node list is non-empty
   *
   * @see FilterExpression.Test
   */
  fun exists(): FilterExpression = FilterExpression.Test(this)

  /**
   * Returns a `count` function expression using this [NodeListExpression] as the argument.
   *
   * @return the `count` function expression
   *
   * @see FunctionExpression.Count
   */
  fun count(): ComparableExpression = FunctionExpression.Count(this)

  /**
   * Returns a `value` function expression using this [NodeListExpression] as the argument.
   *
   * @return the `value` function expression
   *
   * @see FunctionExpression.Count
   */
  fun value(): ComparableExpression = FunctionExpression.Value(this)
}
