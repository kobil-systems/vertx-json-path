package com.kobil.vertx.jsonpath.error

import com.kobil.vertx.jsonpath.JsonNode

data class MultipleResults(
  val results: List<JsonNode>,
)
