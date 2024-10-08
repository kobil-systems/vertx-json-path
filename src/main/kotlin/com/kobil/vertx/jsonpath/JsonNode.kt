package com.kobil.vertx.jsonpath

data class JsonNode(
  val value: Any?,
  val path: JsonPath,
) {
  companion object {
    @JvmStatic
    fun root(value: Any?): JsonNode = JsonNode(value, JsonPath.ROOT)

    val Any?.rootNode: JsonNode
      get() = root(this)
  }
}
