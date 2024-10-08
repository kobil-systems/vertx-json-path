package com.kobil.vertx.jsonpath

sealed interface NodeListExpression : ComparableExpression {
  fun exists(): FilterExpression = FilterExpression.Existence(this)
}
