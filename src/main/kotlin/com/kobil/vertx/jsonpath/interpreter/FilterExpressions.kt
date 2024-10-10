package com.kobil.vertx.jsonpath.interpreter

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import com.kobil.vertx.jsonpath.ComparableExpression
import com.kobil.vertx.jsonpath.FilterExpression
import com.kobil.vertx.jsonpath.FunctionExpression
import com.kobil.vertx.jsonpath.JsonNode
import com.kobil.vertx.jsonpath.NodeListExpression
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.get
import java.util.regex.PatternSyntaxException

fun FilterExpression.test(
  input: JsonNode,
  root: JsonNode = input,
): Boolean =
  when (this) {
    is FilterExpression.And -> this@test.test(input, root)
    is FilterExpression.Or -> this@test.test(input, root)
    is FilterExpression.Not -> !operand.test(input, root)
    is FilterExpression.Comparison -> this@test.test(input, root)
    is FilterExpression.Test -> this@test.test(input, root)
    is FilterExpression.Match -> this@test.test(input, root)
  }

internal fun FilterExpression.And.test(
  input: JsonNode,
  root: JsonNode,
): Boolean =
  operands.fold(true) { acc, operand ->
    acc && operand.test(input, root)
  }

internal fun FilterExpression.Or.test(
  input: JsonNode,
  root: JsonNode,
): Boolean =
  operands.fold(false) { acc, operand ->
    acc || operand.test(input, root)
  }

internal fun FilterExpression.Comparison.test(
  input: JsonNode,
  root: JsonNode,
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

internal fun FilterExpression.Test.test(
  input: JsonNode,
  root: JsonNode,
): Boolean = query.evaluate(input, root).isNotEmpty()

private val unescapedDot = """(?<!\\)(?<backslashes>(\\\\)*)\.(?![^]\n\r]*])""".toRegex()

internal fun FilterExpression.Match.test(
  input: JsonNode,
  root: JsonNode,
): Boolean {
  val subjectStr =
    when (subject) {
      is ComparableExpression.Literal -> subject.value
      is NodeListExpression -> subject.evaluate(input, root).takeIfSingular().getOrNull()
      is FunctionExpression -> pattern.evaluate(input, root)
    } as? String ?: return false

  val patternStr =
    when (pattern) {
      is ComparableExpression.Literal -> pattern.value
      is NodeListExpression -> pattern.evaluate(input, root).takeIfSingular().getOrNull()
      is FunctionExpression -> pattern.evaluate(input, root).getOrNull()
    } as? String ?: return false

  val patternRegex =
    try {
      patternStr.replace(unescapedDot, """${"$"}{backslashes}[^\\n\\r]""").toRegex()
    } catch (pse: PatternSyntaxException) {
      return false
    }

  return if (matchEntire) {
    patternRegex.matches(subjectStr)
  } else {
    patternRegex.containsMatchIn(subjectStr)
  }
}

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
        lhsVal.asSequence().zip(rhsVal.asSequence(), ::valuesEqual).all { it }

    is JsonObject ->
      rhsVal is JsonObject &&
        lhsVal.map.keys == rhsVal.map.keys &&
        lhsVal.map.keys.all { valuesEqual(lhsVal[it], rhsVal[it]) }

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

internal fun replaceNodeListWithNode(maybeList: Any?): Any? {
  val maybeNode =
    (maybeList as? List<*>)
      ?.takeIf { it.size == 1 }
      ?.first()
      as? JsonNode

  return maybeNode ?: maybeList
}
