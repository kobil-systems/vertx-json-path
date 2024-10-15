package com.kobil.vertx.jsonpath

/**
 * A base interface for query-like objects which consist of a sequence of [Segment]s.
 *
 * @param T the query type
 * @see Segment
 * @see Selector
 */
interface JsonPathQuery<T : JsonPathQuery<T>> {
  /**
   * The ordered segments of this query
   */
  val segments: List<Segment>

  /**
   * Appends a child segment using the given selectors to the query.
   *
   * @param selector the first selector
   * @param more additional selectors to include, if any
   * @return a query with the [segments] of this query plus a new child segment
   *
   * @see Segment.ChildSegment
   * @see Selector
   */
  fun selectChildren(
    selector: Selector,
    vararg more: Selector,
  ): T = this + Segment.ChildSegment(selector, *more)

  /**
   * Appends a descendant segment using the given selectors to the query.
   *
   * @param selector the first selector
   * @param more additional selectors to include, if any
   * @return a query with the [segments] of this query plus a new descendant segment
   *
   * @see Segment.DescendantSegment
   * @see Selector
   */
  fun selectDescendants(
    selector: Selector,
    vararg more: Selector,
  ): T = this + Segment.DescendantSegment(selector, *more)

  /**
   * Appends a new wildcard child segment to the query. This selects all direct children of all
   * currently selected nodes
   *
   * @return a query with the [segments] of this query plus a new wildcard child segment
   *
   * @see Segment.ChildSegment
   * @see Selector.Wildcard
   */
  fun selectAllChildren(): T = selectChildren(Selector.WILDCARD)

  /**
   * Appends a new wildcard descendant segment to the query. This will select all currently selected
   * nodes and all their descendants (direct and indirect child nodes)
   *
   * @return a query with the [segments] of this query plus a new wildcard descendant segment
   *
   * @see Segment.DescendantSegment
   * @see Selector.Wildcard
   */
  fun selectAllDescendants(): T = selectDescendants(Selector.WILDCARD)

  /**
   * Selects one or multiple fields from the currently selected nodes.
   *
   * @param field the first field
   * @param more additional fields to include, if any
   * @return a query with the [segments] of this query plus a new child segment using name selectors
   *
   * @see Segment.ChildSegment
   * @See Selector.Name
   */
  fun field(
    field: String,
    vararg more: String,
  ): T = this + Segment.ChildSegment(field, *more)

  /**
   * Selects one or multiple indices from the currently selected nodes.
   *
   * @param index the first index
   * @param more additional indices to include, if any
   * @return a query with the [segments] of this query plus a new child segment
   *   using index selectors
   *
   * @see Segment.ChildSegment
   * @See Selector.Index
   */
  fun index(
    index: Int,
    vararg more: Int,
  ): T = this + Segment.ChildSegment(index, *more)

  /**
   * Appends the given segment to the query.
   *
   * @param segment the segment to append
   * @return a query with the [segments] of this query plus [segment]
   *
   * @see Segment
   */
  operator fun plus(segment: Segment): T

  /**
   * Appends the given segments to the query.
   *
   * @param additionalSegments the segments to append
   * @return a query with the [segments] of this query plus [additionalSegments]
   *
   * @see Segment
   */
  operator fun plus(additionalSegments: Iterable<Segment>): T
}

/**
 * Appends a child segment using the given selectors to the query.
 *
 * @receiver the query to append to
 * @param selector the first selector
 * @param more additional selectors to include, if any
 * @return a query with the [JsonPathQuery.segments] of this query plus a new child segment
 *
 * @see Segment.ChildSegment
 * @see Selector
 */
operator fun <T : JsonPathQuery<T>> T.get(
  selector: Selector,
  vararg more: Selector,
): T = selectChildren(selector, *more)

/**
 * Selects one or multiple fields from the currently selected nodes.
 *
 * @receiver the query to append to
 * @param field the first field
 * @param more additional fields to include, if any
 * @return a query with the [JsonPathQuery.segments] of this query plus a new child segment
 *   using name selectors
 *
 * @see Segment.ChildSegment
 * @See Selector.Name
 */
operator fun <T : JsonPathQuery<T>> T.get(
  field: String,
  vararg more: String,
): T = field(field, *more)

/**
 * Selects one or multiple indices from the currently selected nodes.
 *
 * @receiver the query to append to
 * @param index the first index
 * @param more additional indices to include, if any
 * @return a query with the [JsonPathQuery.segments] of this query plus a new child segment
 *   using index selectors
 *
 * @see Segment.ChildSegment
 * @See Selector.Index
 */
operator fun <T : JsonPathQuery<T>> T.get(
  index: Int,
  vararg more: Int,
): T = index(index, *more)
