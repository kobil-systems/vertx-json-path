package com.kobil.vertx.jsonpath

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.left
import arrow.core.right
import arrow.core.some
import com.kobil.vertx.jsonpath.JsonNode.Companion.rootNode
import com.kobil.vertx.jsonpath.compiler.JsonPathCompiler.compileJsonPathQuery
import com.kobil.vertx.jsonpath.error.JsonPathError
import com.kobil.vertx.jsonpath.error.MultipleResults
import com.kobil.vertx.jsonpath.interpreter.evaluate
import io.vertx.core.json.impl.JsonUtil

data class JsonPath internal constructor(
  val segments: List<Segment> = emptyList(),
) {
  fun evaluate(subject: Any?): List<JsonNode> =
    segments.evaluate(JsonUtil.wrapJsonValue(subject).rootNode)

  fun evaluateOne(subject: Any?): Either<MultipleResults, Option<JsonNode>> =
    evaluate(subject).one()

  operator fun get(
    selector: Selector,
    vararg more: Selector,
  ): JsonPath = JsonPath(segments + Segment.ChildSegment(selector, *more))

  operator fun get(
    field: String,
    vararg more: String,
  ): JsonPath = JsonPath(segments + Segment.ChildSegment(field, *more))

  operator fun get(
    index: Int,
    vararg more: Int,
  ): JsonPath = JsonPath(segments + Segment.ChildSegment(index, *more))

  companion object {
    val root = JsonPath()

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
    ): JsonPath = root.get(selector, *more)

    operator fun get(
      field: String,
      vararg more: String,
    ): JsonPath = root.get(field, *more)

    operator fun get(
      index: Int,
      vararg more: Int,
    ): JsonPath = root.get(index, *more)
  }
}
