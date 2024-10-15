package com.kobil.vertx.jsonpath.error

import com.kobil.vertx.jsonpath.JsonNode

/**
 * A query that was required to yield at most one result returned multiple results.
 *
 * @param results all results that were returned
 *
 * @see JsonNode
 * @see com.kobil.vertx.jsonpath.JsonPath.getOne
 * @see com.kobil.vertx.jsonpath.JsonPath.requireOne
 * @see com.kobil.vertx.jsonpath.JsonPath.traceOne
 * @see com.kobil.vertx.jsonpath.get
 * @see com.kobil.vertx.jsonpath.required
 * @see com.kobil.vertx.jsonpath.traceOne
 */
data class MultipleResults(
  val results: List<JsonNode>,
) : RequiredJsonValueError
