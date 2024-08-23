package com.kobil.vertx.jsonpath

data class JsonNode(
  val value: Any?,
  val path: JsonPath,
) {
  companion object {
    fun root(value: Any?): JsonNode = JsonNode(value, JsonPath.root)

    val Any?.rootNode: JsonNode
      get() = root(this)
  }
}
