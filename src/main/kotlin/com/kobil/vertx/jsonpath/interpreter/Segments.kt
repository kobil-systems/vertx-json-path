package com.kobil.vertx.jsonpath.interpreter

import com.kobil.vertx.jsonpath.Segment
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

fun List<Segment>.evaluate(
  input: Any?,
  root: Any? = input,
): List<Any?> =
  fold(listOf(input)) { selected, segment ->
    segment.evaluate(selected, root)
  }

fun Segment.evaluate(
  input: List<Any?>,
  root: Any?,
): List<Any?> =
  when (this) {
    is Segment.ChildSegment -> evaluate(input, root)
    is Segment.DescendantSegment -> evaluate(input, root)
  }.ifEmpty(::emptyList)

internal fun Segment.ChildSegment.evaluate(
  input: List<Any?>,
  root: Any?,
): List<Any?> =
  input.flatMap { node ->
    selectors.flatMap { it.select(node, root) }
  }

internal fun Segment.DescendantSegment.evaluate(
  input: List<Any?>,
  root: Any?,
): List<Any?> =
  input.flatMap { node ->
    node.enumerateDescendants().flatMap { descendant ->
      selectors.flatMap { it.select(descendant, root) }
    }
  }

internal fun Any?.enumerateDescendants(): Sequence<Any?> =
  sequence {
    yield(this@enumerateDescendants)

    when (this@enumerateDescendants) {
      is JsonObject -> forEach { yieldAll(it.value.enumerateDescendants()) }
      is JsonArray -> forEach { yieldAll(it.enumerateDescendants()) }
    }
  }
