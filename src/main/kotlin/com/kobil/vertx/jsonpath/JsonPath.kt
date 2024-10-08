package com.kobil.vertx.jsonpath

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import arrow.core.some
import com.kobil.vertx.jsonpath.JsonNode.Companion.rootNode
import com.kobil.vertx.jsonpath.compiler.JsonPathCompiler.compileJsonPathQuery
import com.kobil.vertx.jsonpath.error.JsonPathError
import com.kobil.vertx.jsonpath.error.MultipleResults
import com.kobil.vertx.jsonpath.error.RequiredJsonValueError
import com.kobil.vertx.jsonpath.interpreter.evaluate
import io.vertx.core.json.impl.JsonUtil

data class JsonPath internal constructor(
  override val segments: List<Segment> = emptyList(),
) : JsonPathQuery<JsonPath> {
  fun evaluate(subject: Any?): List<JsonNode> =
    segments.evaluate(JsonUtil.wrapJsonValue(subject).rootNode)

  fun evaluateOne(subject: Any?): Either<MultipleResults, Option<JsonNode>> =
    evaluate(subject).one()

  @Suppress("UNCHECKED_CAST")
  fun <T> getAll(subject: Any?): List<T> = evaluate(subject).onlyValues().map { it as T }

  @Suppress("UNCHECKED_CAST")
  fun <T> getOne(subject: Any?): Either<MultipleResults, Option<T>> =
    evaluateOne(subject).map { maybeValue ->
      maybeValue.map { it.value as T }
    }

  @Suppress("UNCHECKED_CAST")
  fun <T> requireOne(subject: Any?): Either<RequiredJsonValueError, T> =
    evaluateOne(subject).flatMap { maybeValue ->
      maybeValue.fold(
        { RequiredJsonValueError.NoResult.left() },
        { (it.value as T).right() },
      )
    }

  fun traceAll(subject: Any?): List<JsonPath> = evaluate(subject).onlyPaths()

  fun traceOne(subject: Any?): Either<MultipleResults, Option<JsonPath>> =
    evaluateOne(subject).map { maybeValue -> maybeValue.map { it.path } }

  override fun plus(segment: Segment): JsonPath = JsonPath(segments + segment)

  override operator fun plus(segments: Iterable<Segment>): JsonPath =
    copy(segments = this.segments + segments)

  override fun toString(): String = segments.joinToString("", prefix = "$")

  companion object {
    @JvmField
    val ROOT = JsonPath()

    @JvmStatic
    fun compile(jsonPath: String): Either<JsonPathError, JsonPath> = compileJsonPathQuery(jsonPath)

    fun List<JsonNode>.one(): Either<MultipleResults, Option<JsonNode>> =
      when (size) {
        0 -> None.right()
        1 -> first().some().right()
        else -> MultipleResults(this).left()
      }

    fun List<JsonNode>.onlyValues(): List<Any?> = map(JsonNode::value)

    fun List<JsonNode>.onlyPaths(): List<JsonPath> = map(JsonNode::path)

    operator fun get(
      selector: Selector,
      vararg more: Selector,
    ): JsonPath = ROOT.get(selector, *more)

    operator fun get(
      field: String,
      vararg more: String,
    ): JsonPath = ROOT.get(field, *more)

    operator fun get(
      index: Int,
      vararg more: Int,
    ): JsonPath = ROOT.get(index, *more)
  }
}
