package com.kobil.vertx.jsonpath

/**
 * Any JSON value together with its position, expressed by a normalized [JsonPath].
 *
 * @param value the JSON value
 * @param path the position of the value, relative to the root document
 * @see JsonPath
 */
data class JsonNode(
  val value: Any?,
  val path: JsonPath,
) {
  /**
   * Contains helpers to obtain certain JSON nodes
   */
  companion object {
    /**
     * Wraps the given value into a JSON node at the root.
     *
     * @param value the JSON value
     * @return a JSON node with the given [value] in root position
     */
    @JvmStatic
    fun root(value: Any?): JsonNode = JsonNode(value, JsonPath.ROOT)

    /**
     * Wraps the given value into a JSON node at the root.
     *
     * @receiver the JSON value
     * @return a JSON node with the given [value] in root position
     */
    val Any?.rootNode: JsonNode
      get() = root(this)
  }
}
