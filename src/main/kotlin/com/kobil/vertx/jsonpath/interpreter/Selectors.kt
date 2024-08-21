package com.kobil.vertx.jsonpath.interpreter

import com.kobil.vertx.jsonpath.Selector
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

fun Selector.select(
  input: Any?,
  root: Any?,
): List<Any?> =
  when (this) {
    is Selector.Name -> select(input)
    is Selector.Index -> select(input)
    is Selector.Wildcard -> selectAll(input)
    is Selector.Slice -> select(input)
    is Selector.Filter -> select(input, root)
  }

internal fun Selector.Name.select(input: Any?): List<Any?> =
  (input as? JsonObject)?.let {
    if (it.containsKey(name)) {
      listOf(it.getValue(name))
    } else {
      emptyList()
    }
  } ?: emptyList()

internal fun selectAll(input: Any?): List<Any?> =
  when (input) {
    is JsonObject -> input.map { it.value }
    is JsonArray -> input.toList()
    else -> emptyList()
  }

internal fun Selector.Index.select(input: Any?): List<Any?> =
  (input as? JsonArray)?.let { arr ->
    val idx = index.normalizeIndex(arr.size())

    if (idx in 0..<arr.size()) {
      listOf(arr.getValue(idx))
    } else {
      emptyList()
    }
  } ?: emptyList()

internal fun Selector.Slice.select(input: Any?): List<Any?> =
  (input as? JsonArray)?.let { arr ->
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

    sequence {
      var i = if (step > 0) lower else upper
      val inBounds = { it: Int -> if (step > 0) it < upper else lower < it }

      while (inBounds(i)) {
        yield(arr.getValue(i))
        i += step
      }
    }.toList()
  } ?: emptyList()

internal fun Selector.Filter.select(
  input: Any?,
  root: Any?,
): List<Any?> =
  when (input) {
    is JsonObject -> input.map { it.value }.filter { filter.match(it, root) }
    is JsonArray -> input.filter { filter.match(it, root) }
    else -> emptyList()
  }

internal fun Int.normalizeIndex(size: Int): Int =
  if (this >= 0) {
    toInt()
  } else {
    toInt() + size
  }
