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
import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

data class JsonPath internal constructor(
  val segments: List<Segment> = emptyList(),
) {
  fun evaluate(obj: JsonObject): List<JsonNode> = segments.evaluate(obj.rootNode)

  fun evaluate(arr: JsonArray): List<JsonNode> = segments.evaluate(arr.rootNode)

  fun evaluateSingle(obj: JsonObject): Either<MultipleResults, Option<JsonNode>> =
    evaluate(obj).one()

  fun evaluateSingle(arr: JsonArray): Either<MultipleResults, Option<JsonNode>> =
    evaluate(arr).one()

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

    suspend fun compile(
      vertx: Vertx,
      jsonPath: String,
    ): Either<JsonPathError, JsonPath> = vertx.compileJsonPathQuery(jsonPath)

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
