package com.kobil.vertx.jsonpath.interpreter

import com.kobil.vertx.jsonpath.JsonNode
import com.kobil.vertx.jsonpath.Segment
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

fun List<Segment>.evaluate(
  input: JsonNode,
  root: JsonNode = input,
): List<JsonNode> =
  fold(listOf(input)) { selected, segment ->
    segment.evaluate(selected, root)
  }

fun Segment.evaluate(
  input: List<JsonNode>,
  root: JsonNode,
): List<JsonNode> =
  when (this) {
    is Segment.ChildSegment -> evaluate(input, root)
    is Segment.DescendantSegment -> evaluate(input, root)
  }.ifEmpty(::emptyList)

internal fun Segment.ChildSegment.evaluate(
  input: List<JsonNode>,
  root: JsonNode,
): List<JsonNode> =
  input.flatMap { selected ->
    selectors.flatMap { it.select(selected, root) }
  }

internal fun Segment.DescendantSegment.evaluate(
  input: List<JsonNode>,
  root: JsonNode,
): List<JsonNode> =
  input.flatMap { selected ->
    selected.enumerateDescendants().flatMap { descendant ->
      selectors.flatMap { it.select(descendant, root) }
    }
  }

internal fun JsonNode.enumerateDescendants(): Sequence<JsonNode> =
  sequence {
    yield(this@enumerateDescendants)

    when (value) {
      is JsonObject ->
        value.forEach { yieldAll(child(it.key, it.value).enumerateDescendants()) }

      is JsonArray ->
        value.forEachIndexed { idx, item ->
          yieldAll(child(idx, item).enumerateDescendants())
        }
    }
  }
