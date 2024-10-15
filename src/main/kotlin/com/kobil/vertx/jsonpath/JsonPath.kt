package com.kobil.vertx.jsonpath

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import arrow.core.some
import com.kobil.vertx.jsonpath.JsonNode.Companion.rootNode
import com.kobil.vertx.jsonpath.compiler.JsonPathCompiler.compileJsonPathQuery
import com.kobil.vertx.jsonpath.error.JsonPathError
import com.kobil.vertx.jsonpath.error.MultipleResults
import com.kobil.vertx.jsonpath.error.RequiredJsonValueError
import com.kobil.vertx.jsonpath.interpreter.evaluate
import io.vertx.core.json.impl.JsonUtil

/**
 * A compiled representation of a JSON Path.
 *
 * @see Segment
 * @see Selector
 */
data class JsonPath internal constructor(
  override val segments: List<Segment> = emptyList(),
) : JsonPathQuery<JsonPath> {
  /**
   * Evaluates this JSON path query on the given [subject]. It returns a list of all nodes matching
   * the JSON Path query.
   *
   * Results are returned as [JsonNode] instances, i.e. JSON values together with their position
   * relative to [subject].
   *
   * @param subject the subject to apply the query to
   * @return the list of matching JSON nodes
   *
   * @see JsonNode
   */
  fun evaluate(subject: Any?): List<JsonNode> =
    segments.evaluate(JsonUtil.wrapJsonValue(subject).rootNode)

  /**
   * Evaluates this JSON path query on the given [subject], requiring that there is at most one
   * result
   *
   * If there is exactly one result, an instance of [Either.Right] containing a [arrow.core.Some]
   * wrapping the node. If there is no result, the [Either.Right] instance contains
   * [arrow.core.None]. If there is more than one result, an [Either.Left] wrapping a
   * [MultipleResults] error is returned.
   *
   * Results are returned as [JsonNode] instances, i.e. JSON values together with their position
   * relative to [subject].
   *
   * @param subject the subject to apply the query to
   * @return the single matching JSON node, if any, or an error
   *
   * @see JsonNode
   * @see MultipleResults
   */
  fun evaluateOne(subject: Any?): Either<MultipleResults, Option<JsonNode>> =
    evaluate(subject).one()

  /**
   * Evaluates this JSON path query on the given [subject]. It returns a list of all values of nodes
   * matching the JSON Path query.
   *
   * @param subject the subject to apply the query to
   * @return the list of matching JSON values
   */
  @Suppress("UNCHECKED_CAST")
  fun <T> getAll(subject: Any?): List<T> = evaluate(subject).onlyValues().map { it as T }

  /**
   * Evaluates this JSON path query on the given [subject], requiring that there is at most one
   * result. The result value is returned, if there is any.
   *
   * If there is exactly one result, an instance of [Either.Right] containing a [arrow.core.Some]
   * wrapping the value. If there is no result, the [Either.Right] instance contains
   * [arrow.core.None]. If there is more than one result, an [Either.Left] wrapping a
   * [MultipleResults] error is returned.
   *
   * @param subject the subject to apply the query to
   * @return the single matching JSON value, if any, or an error
   *
   * @see MultipleResults
   */
  @Suppress("UNCHECKED_CAST")
  fun <T> getOne(subject: Any?): Either<MultipleResults, Option<T>> =
    evaluateOne(subject).map { maybeValue ->
      maybeValue.map { it.value as T }
    }

  /**
   * Evaluates this JSON path query on the given [subject], requiring that there is at most one
   * result. The result value is returned, if there is any.
   *
   * If there is exactly one result, an instance of [Either.Right] containing a [arrow.core.Some]
   * wrapping the value. If there is no result, an [Either.Left] instance containing the
   * [RequiredJsonValueError.NoResult] error is returned. If there is more than one result, an
   * [Either.Left] wrapping a [MultipleResults] error is returned.
   *
   * @param subject the subject to apply the query to
   * @return the single matching JSON value, if any, or an error
   *
   * @see MultipleResults
   * @see RequiredJsonValueError
   */
  @Suppress("UNCHECKED_CAST")
  fun <T> requireOne(subject: Any?): Either<RequiredJsonValueError, T> =
    evaluateOne(subject).flatMap { maybeValue ->
      maybeValue.fold(
        { RequiredJsonValueError.NoResult.left() },
        { (it.value as T).right() },
      )
    }

  /**
   * Evaluates this JSON path query on the given [subject], returning the positions of all matching
   * nodes inside the JSON. The positions are expressed as normalized [JsonPath]s, i.e. JSON Paths
   * only including simple name and index selectors.
   *
   * @param subject the subject to apply the query to
   * @return the list of normalized [JsonPath] positions of matching nodes
   */
  fun traceAll(subject: Any?): List<JsonPath> = evaluate(subject).onlyPaths()

  /**
   * Evaluates this JSON path query on the given [subject], requiring that there is at most one
   * result. The position of the result is returned, if any. The position is expressed as a
   * normalized [JsonPath], i.e. a JSON Paths only including simple name and index selectors.
   *
   * If there is exactly one result, an instance of [Either.Right] containing a [arrow.core.Some]
   * wrapping the position. If there is no result, the [Either.Right] instance contains
   * [arrow.core.None]. If there is more than one result, an [Either.Left] wrapping a
   * [MultipleResults] error is returned.
   *
   * @param subject the subject to apply the query to
   * @return the position of the single matching JSON node, if any, or an error
   *
   * @see MultipleResults
   */
  fun traceOne(subject: Any?): Either<MultipleResults, Option<JsonPath>> =
    evaluateOne(subject).map { maybeValue -> maybeValue.map { it.path } }

  override fun plus(segment: Segment): JsonPath = JsonPath(segments + segment)

  override operator fun plus(additionalSegments: Iterable<Segment>): JsonPath =
    copy(segments = this.segments + additionalSegments)

  /**
   * Serializes this JSON path to the corresponding string representation.
   */
  override fun toString(): String = segments.joinToString("", prefix = "$")

  /**
   * Contains various utilities to work with JSON Paths
   */
  companion object {
    /**
     * The [JsonPath] referring to the root document, i.e. '$'
     */
    @JvmField
    val ROOT = JsonPath()

    /**
     * Compiles a filter expression string. It will return an instance of [Either.Right] containing
     * the compiled [JsonPath] if the compilation was successful. Otherwise, an error wrapped in an
     * instance of [Either.Left] is returned.
     *
     * @param jsonPath the JSON Path string
     * @return the compiled [JsonPath], or an error
     * @see JsonPathError
     */
    @JvmStatic
    fun compile(jsonPath: String): Either<JsonPathError, JsonPath> = compileJsonPathQuery(jsonPath)

    private fun List<JsonNode>.one(): Either<MultipleResults, Option<JsonNode>> =
      when (size) {
        0 -> None.right()
        1 -> first().some().right()
        else -> MultipleResults(this).left()
      }

    /**
     * Only keeps the values from the list of results
     *
     * @receiver a list of [JsonNode]
     * @return the list of values of the JSON nodes
     * @see [JsonNode]
     */
    fun List<JsonNode>.onlyValues(): List<Any?> = map(JsonNode::value)

    /**
     * Only keeps the positions from the list of results
     *
     * @receiver a list of [JsonNode]
     * @return the list of positions of the JSON nodes
     * @see [JsonNode]
     */
    fun List<JsonNode>.onlyPaths(): List<JsonPath> = map(JsonNode::path)

    /**
     * Returns a JSON path with one child segment using the given selectors.
     *
     * @param selector the first selector
     * @param more additional selectors to include, if any
     * @return A [JsonPath], equivalent to  $[selector, ...more]
     *
     * @see Segment.ChildSegment
     * @See Selector
     */
    operator fun get(
      selector: Selector,
      vararg more: Selector,
    ): JsonPath = ROOT.get(selector, *more)

    /**
     * Returns a JSON path with one child segment selecting the given field names.
     *
     * @param field the first field
     * @param more additional fields to include, if any
     * @return A [JsonPath], equivalent to  $['field', ...'more']
     *
     * @see Segment.ChildSegment
     * @See Selector.Name
     */
    operator fun get(
      field: String,
      vararg more: String,
    ): JsonPath = ROOT.get(field, *more)

    /**
     * Returns a JSON path with one child segment selecting the given indices.
     *
     * @param index the first index
     * @param more additional indices to include, if any
     * @return A [JsonPath], equivalent to  $[index, ...more]
     *
     * @see Segment.ChildSegment
     * @See Selector.Index
     */
    operator fun get(
      index: Int,
      vararg more: Int,
    ): JsonPath = ROOT.get(index, *more)
  }
}
