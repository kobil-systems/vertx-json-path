package com.kobil.vertx.jsonpath.interpreter

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.core.getOrElse
import com.kobil.vertx.jsonpath.FilterExpression
import com.kobil.vertx.jsonpath.FunctionExpression
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

fun FilterExpression.match(
  input: Any?,
  root: Any? = input,
): Boolean =
  when (this) {
    is FilterExpression.And -> match(input, root)
    is FilterExpression.Or -> match(input, root)
    is FilterExpression.Not -> !operand.match(input, root)
    is FilterExpression.Comparison -> match(input, root)
    is FilterExpression.Existence -> match(input, root)
    is FunctionExpression.Match -> match(input, root)
  }

internal fun FilterExpression.And.match(
  input: Any?,
  root: Any?,
): Boolean =
  operands.fold(true) { acc, operand ->
    acc && operand.match(input, root)
  }

internal fun FilterExpression.Or.match(
  input: Any?,
  root: Any?,
): Boolean =
  operands.fold(false) { acc, operand ->
    acc || operand.match(input, root)
  }

internal fun FilterExpression.Comparison.match(
  input: Any?,
  root: Any?,
): Boolean {
  val lhsVal = lhs.evaluate(input, root)
  val rhsVal = rhs.evaluate(input, root)

  return when (op) {
    FilterExpression.Comparison.Op.EQ -> equals(lhsVal, rhsVal)
    FilterExpression.Comparison.Op.NOT_EQ -> !equals(lhsVal, rhsVal)
    FilterExpression.Comparison.Op.LESS -> less(lhsVal, rhsVal)
    FilterExpression.Comparison.Op.LESS_EQ -> less(lhsVal, rhsVal) || equals(lhsVal, rhsVal)
    FilterExpression.Comparison.Op.GREATER -> greater(lhsVal, rhsVal)
    FilterExpression.Comparison.Op.GREATER_EQ -> greater(lhsVal, rhsVal) || equals(lhsVal, rhsVal)
  }
}

internal fun FilterExpression.Existence.match(
  input: Any?,
  root: Any?,
): Boolean = query.evaluate(input, root).isNotEmpty()

internal fun FunctionExpression.Match.match(
  input: Any?,
  root: Any?,
): Boolean = evaluate(input, root).getOrElse { false }

internal fun equals(
  lhs: Option<Any?>,
  rhs: Option<Any?>,
): Boolean =
  when (lhs) {
    is None -> rhs is None
    is Some -> rhs is Some && valuesEqual(lhs.value, rhs.value)
  }

internal fun less(
  lhs: Option<Any?>,
  rhs: Option<Any?>,
): Boolean =
  when (lhs) {
    is None -> false
    is Some -> rhs is Some && valueLess(lhs.value, rhs.value)
  }

internal fun greater(
  lhs: Option<Any?>,
  rhs: Option<Any?>,
): Boolean =
  when (lhs) {
    is None -> false
    is Some -> rhs is Some && valueGreater(lhs.value, rhs.value)
  }

internal fun valuesEqual(
  lhs: Any?,
  rhs: Any?,
): Boolean {
  val lhsVal = replaceNodeListWithNode(lhs)
  val rhsVal = replaceNodeListWithNode(rhs)

  return when (lhsVal) {
    null -> rhs == null
    is Number -> rhsVal is Number && lhsVal.toDouble() == rhsVal.toDouble()
    is String -> rhsVal is String && lhsVal == rhsVal
    is Boolean -> rhsVal is Boolean && lhsVal == rhsVal
    is JsonArray ->
      rhsVal is JsonArray &&
        lhsVal.size() == rhsVal.size() &&
        lhsVal
          .zip(rhsVal)
          .all { (l, r) -> valuesEqual(l, r) }

    is JsonObject ->
      rhsVal is JsonObject &&
        lhsVal.map.keys == rhsVal.map.keys &&
        lhsVal.map.keys.all {
          valuesEqual(
            lhsVal.getValue(it),
            rhsVal.getValue(it),
          )
        }

    is List<*> ->
      if (lhsVal.isEmpty()) {
        rhsVal is List<*> && rhsVal.isEmpty()
      } else {
        false
      }

    else -> false
  }
}

internal fun valueLess(
  lhs: Any?,
  rhs: Any?,
): Boolean =
  when (lhs) {
    is Number -> rhs is Number && lhs.toDouble() < rhs.toDouble()
    is String -> rhs is String && lhs < rhs
    else -> false
  }

internal fun valueGreater(
  lhs: Any?,
  rhs: Any?,
): Boolean =
  when (lhs) {
    is Number -> rhs is Number && lhs.toDouble() > rhs.toDouble()
    is String -> rhs is String && lhs > rhs
    else -> false
  }

internal fun replaceNodeListWithNode(maybeList: Any?): Any? =
  (maybeList as? List<Any?>)?.takeIf { it.size == 1 }?.first() ?: maybeList
