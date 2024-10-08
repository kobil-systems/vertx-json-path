package com.kobil.vertx.jsonpath.interpreter

import com.kobil.vertx.jsonpath.JsonNode
import com.kobil.vertx.jsonpath.Selector
import com.kobil.vertx.jsonpath.get
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

fun Selector.select(
  input: JsonNode,
  root: JsonNode,
): List<JsonNode> =
  when (this) {
    is Selector.Name -> select(input)
    is Selector.Index -> select(input)
    is Selector.Wildcard -> selectAll(input)
    is Selector.Slice -> select(input)
    is Selector.Filter -> select(input, root)
  }

internal fun Selector.Name.select(input: JsonNode): List<JsonNode> =
  (input.value as? JsonObject)?.let {
    if (it.containsKey(name)) {
      listOf(input.child(name, it.getValue(name)))
    } else {
      emptyList()
    }
  } ?: emptyList()

internal fun selectAll(input: JsonNode): List<JsonNode> =
  when (val node = input.value) {
    is JsonObject -> node.map { input.child(it.key, it.value) }
    is JsonArray -> node.mapIndexed { idx, item -> input.child(idx, item) }
    else -> emptyList()
  }

internal fun Selector.Index.select(input: JsonNode): List<JsonNode> =
  (input.value as? JsonArray)?.let { arr ->
    val idx = index.normalizeIndex(arr.size())

    if (idx in 0..<arr.size()) {
      listOf(input.child(idx, arr.getValue(idx)))
    } else {
      emptyList()
    }
  } ?: emptyList()

internal fun Selector.Slice.select(input: JsonNode): List<JsonNode> =
  (input.value as? JsonArray)?.let { arr ->
    val len = arr.size()
    val step = step ?: 1

    if (step == 0) return emptyList()

    val lower =
      if (step > 0) {
        minOf(maxOf(first?.normalizeIndex(len) ?: 0, 0), len)
      } else {
        minOf(maxOf(last?.normalizeIndex(len) ?: -1, -1), len - 1)
      }

    val upper =
      if (step > 0) {
        minOf(maxOf(last?.normalizeIndex(len) ?: len, 0), len)
      } else {
        minOf(maxOf(first?.normalizeIndex(len) ?: (len - 1), -1), len - 1)
      }

    val range =
      if (step > 0) {
        lower..<upper step step
      } else {
        upper downTo lower + 1 step -step
      }

    range.map { i -> input.child(i, arr.getValue(i)) }
  } ?: emptyList()

internal fun Selector.Filter.select(
  input: JsonNode,
  root: JsonNode,
): List<JsonNode> =
  when (val value = input.value) {
    is JsonObject ->
      value
        .asSequence()
        .map { (key, field) -> input.child(key, field) }
        .filter { node -> filter.test(node, root) }
        .toList()

    is JsonArray ->
      value
        .asSequence()
        .mapIndexed { idx, item -> input.child(idx, item) }
        .filter { node -> filter.test(node, root) }
        .toList()

    else -> emptyList()
  }

internal fun Int.normalizeIndex(size: Int): Int =
  if (this >= 0) {
    toInt()
  } else {
    toInt() + size
  }

internal fun JsonNode.child(
  name: String,
  node: Any?,
): JsonNode = JsonNode(node, path[name])

internal fun JsonNode.child(
  index: Int,
  node: Any?,
): JsonNode = JsonNode(node, path[index])
