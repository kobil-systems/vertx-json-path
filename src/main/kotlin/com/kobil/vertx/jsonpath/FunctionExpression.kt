package com.kobil.vertx.jsonpath

sealed class FunctionExpression : ComparableExpression {
  data class Length(
    val arg: ComparableExpression,
  ) : FunctionExpression() {
    override fun toString(): String = "length($arg)"
  }

  data class Count(
    val arg: QueryExpression<*>,
  ) : FunctionExpression() {
    override fun toString(): String = "count($arg)"
  }

  data class Value(
    val arg: QueryExpression<*>,
  ) : FunctionExpression() {
    override fun toString(): String = "value($arg)"
  }
}
