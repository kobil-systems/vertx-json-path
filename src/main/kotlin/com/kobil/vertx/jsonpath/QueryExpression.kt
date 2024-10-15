package com.kobil.vertx.jsonpath

/**
 * A query expression, that is, a JSON Path query or a relative query which occurs as part of a
 * [FilterExpression]. It may be used in test expressions (checking for existence), as an argument
 * to function expressions, or as an operand in comparisons.
 *
 * @see NodeListExpression
 * @see FilterExpression.Comparison
 * @see FilterExpression.Match
 * @see FunctionExpression
 *
 * @param T the concrete query type
 */
sealed interface QueryExpression<T : QueryExpression<T>> :
  NodeListExpression,
  JsonPathQuery<T> {
  /**
   * Indicates whether the query is a singular query, that is, it contains only child segments with
   * only one [Selector.Name] or [Selector.Index] selector each.
   *
   * @see Segment.ChildSegment
   */
  val isSingular: Boolean
    get() = segments.all { it.isSingular }

  /**
   * A relative query, i.e. a JSON path expression starting with '@'.
   *
   * @param segments the segments of the query
   * @see Segment
   */
  data class Relative
    @JvmOverloads
    constructor(
      override val segments: List<Segment> = listOf(),
    ) : QueryExpression<Relative> {
      /**
       * An alternative varargs constructor
       *
       * @param firstSegment the first segment of the query
       * @param more additional segments, if any
       * @see Segment
       */
      constructor(firstSegment: Segment, vararg more: Segment) : this(listOf(firstSegment, *more))

      override operator fun plus(segment: Segment): Relative = copy(segments = segments + segment)

      override operator fun plus(additionalSegments: Iterable<Segment>): Relative =
        copy(segments = this.segments + additionalSegments)

      /**
       * Serializes this query to the corresponding string representation
       */
      override fun toString(): String = segments.joinToString("", prefix = "@")
    }

  /**
   * An absolute query, equivalent to a JSON Path. In the context of a [FilterExpression], it refers
   * to the root document
   *
   * @param segments the segments of the query
   * @see Segment
   */
  data class Absolute
    @JvmOverloads
    constructor(
      override val segments: List<Segment> = listOf(),
    ) : QueryExpression<Absolute> {
      /**
       * An alternative varargs constructor
       *
       * @param firstSegment the first segment of the query
       * @param more additional segments, if any
       * @see Segment
       */
      constructor(firstSegment: Segment, vararg more: Segment) : this(listOf(firstSegment, *more))

      override operator fun plus(segment: Segment): Absolute = copy(segments = segments + segment)

      override operator fun plus(additionalSegments: Iterable<Segment>): Absolute =
        copy(segments = this.segments + additionalSegments)

      /**
       * Serializes this query to the corresponding string representation
       */
      override fun toString(): String = segments.joinToString("", prefix = "$")
    }

  /**
   * Contains convenience factory functions
   */
  companion object {
    /**
     * Returns a relative query with the given segments.
     *
     * @param segments the segments to use
     * @return the relative query
     *
     * @see Segment
     * @see Relative
     */
    @JvmStatic
    @JvmOverloads
    fun relative(segments: List<Segment> = listOf()): Relative = Relative(segments)

    /**
     * Returns a relative query with the given segments.
     *
     * @param firstSegment the first segment of the query
     * @param more additional segments, if any
     * @return the relative query
     *
     * @see Segment
     * @see Relative
     */
    @JvmStatic
    fun relative(
      firstSegment: Segment,
      vararg more: Segment,
    ): Relative = Relative(firstSegment, *more)

    /**
     * Returns an absolute query with the given segments.
     *
     * @param segments the segments to use
     * @return the relative query
     *
     * @see Segment
     * @see Absolute
     */
    @JvmStatic
    @JvmOverloads
    fun absolute(segments: List<Segment> = listOf()): Absolute = Absolute(segments)

    /**
     * Returns an absolute query with the given segments.
     *
     * @param firstSegment the first segment of the query
     * @param more additional segments, if any
     * @return the absolute query
     *
     * @see Segment
     * @see Absolute
     */
    @JvmStatic
    fun absolute(
      firstSegment: Segment,
      vararg more: Segment,
    ): Absolute = Absolute(firstSegment, *more)
  }
}
