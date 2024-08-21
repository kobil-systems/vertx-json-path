package com.kobil.vertx.jsonpath.interpreter

import arrow.core.None
import arrow.core.Option
import arrow.core.raise.option
import arrow.core.some
import com.kobil.vertx.jsonpath.ComparableExpression
import com.kobil.vertx.jsonpath.FunctionExpression
import com.kobil.vertx.jsonpath.NodeListExpression
import com.kobil.vertx.jsonpath.QueryExpression
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import java.util.regex.PatternSyntaxException

internal fun ComparableExpression.evaluate(
  input: Any?,
  root: Any?,
): Option<Any?> =
  when (this) {
    is ComparableExpression.Literal -> value.some()
    is FunctionExpression.Length -> evaluate(input, root)
    is FunctionExpression.Count -> evaluate(input, root)
    is FunctionExpression.Value -> evaluate(input, root)
    is FunctionExpression.Match -> evaluate(input, root)
    is QueryExpression -> evaluate(input, root).takeIfSingular()
  }

internal fun FunctionExpression.Length.evaluate(
  input: Any?,
  root: Any?,
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
  input: Any?,
  root: Any?,
): Option<Any?> = arg.evaluate(input, root).size.some()

internal fun FunctionExpression.Value.evaluate(
  input: Any?,
  root: Any?,
): Option<Any?> = arg.evaluate(input, root).takeIfSingular()

private val unescapedDot = """(?<!\\)(?<backslashes>(\\\\)*)\.(?![^]\n\r]*])""".toRegex()

internal fun FunctionExpression.Match.evaluate(
  input: Any?,
  root: Any?,
): Option<Boolean> =
  option {
    val subjectStr =
      when (subject) {
        is ComparableExpression.Literal -> subject.value
        is NodeListExpression -> subject.evaluate(input, root).takeIfSingular().getOrNull()
        is FunctionExpression -> pattern.evaluate(input, root)
      } as? String ?: return@option false

    val patternStr =
      when (pattern) {
        is ComparableExpression.Literal -> pattern.value
        is NodeListExpression -> pattern.evaluate(input, root).takeIfSingular().getOrNull()
        is FunctionExpression -> pattern.evaluate(input, root).getOrNull()
      } as? String ?: return@option false

    val patternRegex =
      try {
        patternStr.replace(unescapedDot, """${"$"}{backslashes}[^\\n\\r]""").toRegex()
      } catch (pse: PatternSyntaxException) {
        return@option false
      }

    if (full) {
      patternRegex.matches(subjectStr)
    } else {
      patternRegex.containsMatchIn(subjectStr)
    }
  }

internal fun NodeListExpression.evaluate(
  input: Any?,
  root: Any?,
): List<Any?> =
  when (this) {
    is QueryExpression -> evaluate(input, root)
  }

internal fun QueryExpression.evaluate(
  input: Any?,
  root: Any?,
): List<Any?> =
  when (this) {
    is QueryExpression.Relative -> segments.evaluate(input, root)
    is QueryExpression.Absolute -> segments.evaluate(root)
  }

internal fun List<Any?>.takeIfSingular(): Option<Any?> =
  if (size == 1) {
    first().some()
  } else {
    None
  }
