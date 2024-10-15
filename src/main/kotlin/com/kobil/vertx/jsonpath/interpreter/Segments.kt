package com.kobil.vertx.jsonpath.interpreter

import com.kobil.vertx.jsonpath.JsonNode
import com.kobil.vertx.jsonpath.Segment
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

/**
 * Evaluates the segments on the given [input] node in the context of the [root] node. If
 * any selector within the segments contains nested absolute queries, they are evaluated on [root].
 *
 * @receiver an ordered list of segments to evaluate
 * @param input the input node
 * @param root the root node. When omitted, `input == root` is assumed.
 * @return the resulting node list after all segments have been evaluated
 *
 * @see JsonNode
 * @see Segment
 */
@JvmOverloads
fun List<Segment>.evaluate(
  input: JsonNode,
  root: JsonNode = input,
): List<JsonNode> =
  fold(listOf(input)) { selected, segment ->
    segment.evaluate(selected, root)
  }

/**
 * Evaluates the segment on the given [input] node list in the context of the [root] node. If
 * any selector within the segments contains nested absolute queries, they are evaluated on [root].
 *
 * @receiver the segment to evaluate
 * @param input the input node list
 * @param root the root node
 * @return the resulting node list after the segment has been evaluated
 *
 * @see JsonNode
 * @see Segment
 */
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
