package com.kobil.vertx.jsonpath.interpreter

import arrow.core.None
import arrow.core.Option
import arrow.core.some
import com.kobil.vertx.jsonpath.ComparableExpression
import com.kobil.vertx.jsonpath.FunctionExpression
import com.kobil.vertx.jsonpath.JsonNode
import com.kobil.vertx.jsonpath.JsonPath.Companion.onlyValues
import com.kobil.vertx.jsonpath.NodeListExpression
import com.kobil.vertx.jsonpath.QueryExpression
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

internal fun ComparableExpression.evaluate(
  input: JsonNode,
  root: JsonNode,
): Option<Any?> =
  when (this) {
    is ComparableExpression.Literal -> value.some()
    is FunctionExpression.Length -> evaluate(input, root)
    is FunctionExpression.Count -> evaluate(input, root)
    is FunctionExpression.Value -> evaluate(input, root)
    is QueryExpression<*> -> evaluate(input, root).takeIfSingular()
  }

internal fun FunctionExpression.Length.evaluate(
  input: JsonNode,
  root: JsonNode,
): Option<Any?> =
  when (arg) {
    is ComparableExpression.Literal -> arg.value.some()
    is NodeListExpression -> arg.evaluate(input, root).takeIfSingular()
    is FunctionExpression -> arg.evaluate(input, root)
  }.flatMap {
    when (it) {
      is String -> it.length.some()
      is JsonArray -> it.size().some()
      is JsonObject -> it.size().some()
      else -> None
    }
  }

internal fun FunctionExpression.Count.evaluate(
  input: JsonNode,
  root: JsonNode,
): Option<Any?> = arg.evaluate(input, root).size.some()

internal fun FunctionExpression.Value.evaluate(
  input: JsonNode,
  root: JsonNode,
): Option<Any?> = arg.evaluate(input, root).takeIfSingular()

internal fun NodeListExpression.evaluate(
  input: JsonNode,
  root: JsonNode,
): List<Any?> =
  when (this) {
    is QueryExpression<*> -> evaluate(input, root)
  }

internal fun QueryExpression<*>.evaluate(
  input: JsonNode,
  root: JsonNode,
): List<Any?> =
  when (this) {
    is QueryExpression.Relative ->
      segments.evaluate(input, root).onlyValues()

    is QueryExpression.Absolute ->
      segments.evaluate(root).onlyValues()
  }

internal fun List<Any?>.takeIfSingular(): Option<Any?> =
  if (size == 1) {
    first().some()
  } else {
    None
  }
