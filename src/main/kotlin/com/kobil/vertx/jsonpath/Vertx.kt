package com.kobil.vertx.jsonpath

import arrow.core.Either
import arrow.core.Option
import com.kobil.vertx.jsonpath.JsonPath.Companion.onlyValues
import com.kobil.vertx.jsonpath.error.MultipleResults
import com.kobil.vertx.jsonpath.error.RequiredJsonValueError
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

/**
 * Queries the JSON object using the JSON path [path] and returns the single result, if any. If
 * there are multiple results, an error is returned.
 *
 * @param path the JSON path query to use
 * @return the result, if there is exactly one. None if there is no result. An error otherwise.
 *
 * @see JsonPath.getOne
 */
inline operator fun <reified T> JsonObject.get(path: JsonPath): Either<MultipleResults, Option<T>> =
  path.getOne(this)

/**
 * Queries the JSON array using the JSON path [path] and returns the single result, if any. If
 * there are multiple results, an error is returned.
 *
 * @param path the JSON path query to use
 * @return the result, if there is exactly one. None if there is no result. An error otherwise.
 *
 * @see JsonPath.getOne
 */
inline operator fun <reified T> JsonArray.get(path: JsonPath): Either<MultipleResults, Option<T>> =
  path.getOne(this)

/**
 * Queries the JSON object using the JSON path [path] and returns the single result. If
 * there are multiple results, an error is returned. If there is no result, an error is returned.
 *
 * @param path the JSON path query to use
 * @return the result, if there is exactly one. An error otherwise.
 *
 * @see JsonPath.requireOne
 */
inline fun <reified T> JsonObject.required(path: JsonPath): Either<RequiredJsonValueError, T> =
  path.requireOne(this)

/**
 * Queries the JSON array using the JSON path [path] and returns the single result. If
 * there are multiple results, an error is returned. If there is no result, an error is returned.
 *
 * @param path the JSON path query to use
 * @return the result, if there is exactly one. An error otherwise.
 *
 * @see JsonPath.requireOne
 */
inline fun <reified T> JsonArray.required(path: JsonPath): Either<RequiredJsonValueError, T> =
  path.requireOne(this)

/**
 * Queries the JSON object using the JSON path [path] and returns all results.
 *
 * @param path the JSON path query to use
 * @return the results
 *
 * @see JsonPath.getAll
 */
inline fun <reified T> JsonObject.getAll(path: JsonPath): List<T> =
  path.evaluate(this).onlyValues().map { it as T }

/**
 * Queries the JSON array using the JSON path [path] and returns all results.
 *
 * @param path the JSON path query to use
 * @return the results
 *
 * @see JsonPath.getAll
 */
inline fun <reified T> JsonArray.getAll(path: JsonPath): List<T> =
  path.evaluate(this).onlyValues().map { it as T }

/**
 * Queries the JSON object using the JSON path [path] and returns the position of the single result,
 * if any. If there are multiple results, an error is returned.
 *
 * @param path the JSON path query to use
 * @return the position of the result, if there is exactly one. None if there is no result.
 *   An error otherwise.
 *
 * @see JsonPath.traceOne
 */
fun JsonObject.traceOne(path: JsonPath): Either<MultipleResults, Option<JsonPath>> =
  path.traceOne(this)

/**
 * Queries the JSON array using the JSON path [path] and returns the position of the single result,
 * if any. If there are multiple results, an error is returned.
 *
 * @param path the JSON path query to use
 * @return the position of the result, if there is exactly one. None if there is no result.
 *   An error otherwise.
 *
 * @see JsonPath.traceOne
 */
fun JsonArray.traceOne(path: JsonPath): Either<MultipleResults, Option<JsonPath>> =
  path.traceOne(this)

/**
 * Queries the JSON object using the JSON path [path] and returns the positions of all results.
 *
 * @param path the JSON path query to use
 * @return the positions of the results
 *
 * @see JsonPath.traceAll
 */
fun JsonObject.traceAll(path: JsonPath): List<JsonPath> = path.traceAll(this)

/**
 * Queries the JSON object using the JSON path [path] and returns the positions of all results.
 *
 * @param path the JSON path query to use
 * @return the positions of the results
 *
 * @see JsonPath.traceAll
 */
fun JsonArray.traceAll(path: JsonPath): List<JsonPath> = path.traceAll(this)
