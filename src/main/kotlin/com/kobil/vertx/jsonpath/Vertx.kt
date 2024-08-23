package com.kobil.vertx.jsonpath

import arrow.core.Either
import arrow.core.Option
import arrow.core.flatMap
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import com.kobil.vertx.jsonpath.JsonPath.Companion.onlyPaths
import com.kobil.vertx.jsonpath.JsonPath.Companion.onlyValues
import com.kobil.vertx.jsonpath.error.MultipleResults
import com.kobil.vertx.jsonpath.error.RequiredJsonValueError
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

inline operator fun <reified T> JsonObject.get(path: JsonPath): Either<MultipleResults, Option<T>> =
  path.evaluateOne(this).map { node ->
    node.map { it.value as T }
  }

inline operator fun <reified T> JsonArray.get(path: JsonPath): Either<MultipleResults, Option<T>> =
  path.evaluateOne(this).map { node ->
    node.map { it.value as T }
  }

inline fun <reified T> JsonObject.required(path: JsonPath): Either<RequiredJsonValueError, T> {
  val maybeNode = path.evaluateOne(this) as Either<RequiredJsonValueError, Option<JsonNode>>

  return maybeNode.flatMap { node ->
    node.map { (it.value as T).right() }.getOrElse { RequiredJsonValueError.NoResult.left() }
  }
}

inline fun <reified T> JsonArray.required(path: JsonPath): Either<RequiredJsonValueError, T> {
  val maybeNode = path.evaluateOne(this) as Either<RequiredJsonValueError, Option<JsonNode>>

  return maybeNode.flatMap { node ->
    node.map { (it.value as T).right() }.getOrElse { RequiredJsonValueError.NoResult.left() }
  }
}

inline fun <reified T> JsonObject.getAll(path: JsonPath): List<T> =
  path.evaluate(this).onlyValues().map { it as T }

inline fun <reified T> JsonArray.getAll(path: JsonPath): List<T> =
  path.evaluate(this).onlyValues().map { it as T }

fun JsonObject.traceOne(path: JsonPath): Either<MultipleResults, Option<JsonPath>> =
  path.evaluateOne(this).map { node -> node.map(JsonNode::path) }

fun JsonArray.traceOne(path: JsonPath): Either<MultipleResults, Option<JsonPath>> =
  path.evaluateOne(this).map { node -> node.map(JsonNode::path) }

fun JsonObject.traceAll(path: JsonPath): List<JsonPath> = path.evaluate(this).onlyPaths()

fun JsonArray.traceAll(path: JsonPath): List<JsonPath> = path.evaluate(this).onlyPaths()
