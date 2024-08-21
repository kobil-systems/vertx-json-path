package com.kobil.vertx.jsonpath

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.left
import arrow.core.right
import arrow.core.some
import com.kobil.vertx.jsonpath.compiler.JsonPathCompiler.compileJsonPathQuery
import com.kobil.vertx.jsonpath.error.JsonPathError
import com.kobil.vertx.jsonpath.error.MultipleResults
import com.kobil.vertx.jsonpath.interpreter.evaluate
import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

data class JsonPath(val segments: List<Segment>) {
  fun evaluate(obj: JsonObject): List<Any?> = segments.evaluate(obj)

  fun evaluate(arr: JsonArray): List<Any?> = segments.evaluate(arr)

  fun evaluateSingle(obj: JsonObject): Either<MultipleResults, Option<Any?>> =
    segments.evaluate(obj).one()

  fun evaluateSingle(arr: JsonArray): Either<MultipleResults, Option<Any?>> =
    segments.evaluate(arr).one()

  companion object {
    suspend fun compile(
      vertx: Vertx,
      jsonPath: String,
    ): Either<JsonPathError, JsonPath> = vertx.compileJsonPathQuery(jsonPath)

    fun List<Any?>.one(): Either<MultipleResults, Option<Any?>> =
      when (size) {
        0 -> None.right()
        1 -> first().some().right()
        else -> MultipleResults(this).left()
      }
  }
}
