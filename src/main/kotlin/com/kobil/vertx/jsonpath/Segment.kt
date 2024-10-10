package com.kobil.vertx.jsonpath

/**
 * A segment of a JSON path query. It includes [Selector]s, which are applied to the input JSON
 * separately, concatenating the results. Segments may either be [ChildSegment] or
 * [DescendantSegment], with the former only selecting direct descendants of the currently selected
 * nodes and the latter selecting the current nodes and all direct or indirect descendants.
 *
 * @see Selector
 */
sealed interface Segment {
  /**
   * The selectors used by the segment
   */
  val selectors: List<Selector>

  /**
   * Whether this segment selects at most one result
   */
  val isSingular: Boolean

  /**
   * A child segment selecting direct children of the currently selected nodes.
   *
   * @param selectors the selectors used by the segment
   *
   * @see Selector
   */
  data class ChildSegment(
    override val selectors: List<Selector>,
  ) : Segment {
    init {
      require(selectors.isNotEmpty()) { "A child segment without selectors is not allowed" }
    }

    /**
     * An alternative vararg constructor
     *
     * @param selector the first selector
     * @param more additional selectors, if any
     *
     * @see Selector
     */
    constructor(selector: Selector, vararg more: Selector) : this(listOf(selector, *more))

    /**
     * An alternative vararg constructor, allowing to specify only field names
     *
     * @param field the first field name to select
     * @param more additional field names, if any
     *
     * @see Selector.Name
     */
    constructor(field: String, vararg more: String) : this(
      Selector.Name(field),
      *more.map(Selector::Name).toTypedArray(),
    )

    /**
     * An alternative vararg constructor, allowing to specify only indices
     *
     * @param index the first index to select
     * @param more additional indices, if any
     *
     * @see Selector.Index
     */
    constructor(index: Int, vararg more: Int) : this(
      Selector.Index(index),
      *more.map(Selector::Index).toTypedArray(),
    )

    override val isSingular: Boolean
      get() =
        selectors.size == 1 &&
          (selectors.first() is Selector.Name || selectors.first() is Selector.Index)

    override fun toString(): String = selectors.joinToString(",", prefix = "[", postfix = "]")
  }

  /**
   * A descendant segment selecting direct and indirect children of the currently selected nodes.
   *
   * @param selectors the selectors used by the segment
   *
   * @see Selector
   */
  data class DescendantSegment(
    override val selectors: List<Selector>,
  ) : Segment {
    init {
      require(selectors.isNotEmpty()) { "A descendant segment without selectors is not allowed" }
    }

    /**
     * An alternative vararg constructor
     *
     * @param selector the first selector
     * @param more additional selectors, if any
     *
     * @see Selector
     */
    constructor(selector: Selector, vararg more: Selector) : this(listOf(selector, *more))

    /**
     * An alternative vararg constructor, allowing to specify only field names
     *
     * @param field the first field name to select
     * @param more additional field names, if any
     *
     * @see Selector.Name
     */
    constructor(field: String, vararg more: String) : this(
      Selector.Name(field),
      *more.map(Selector::Name).toTypedArray(),
    )

    /**
     * An alternative vararg constructor, allowing to specify only indices
     *
     * @param index the first index to select
     * @param more additional indices, if any
     *
     * @see Selector.Index
     */
    constructor(index: Int, vararg more: Int) : this(
      Selector.Index(index),
      *more.map(Selector::Index).toTypedArray(),
    )

    override val isSingular: Boolean
      get() = false

    override fun toString(): String = selectors.joinToString(",", prefix = "..[", postfix = "]")
  }

  /**
   * Contains useful factory functions
   */
  companion object {
    /**
     * Creates a new [ChildSegment] with the given selectors.
     *
     * @param selectors the selectors to use
     * @return the child segment
     *
     * @see ChildSegment
     * @see Selector
     */
    @JvmStatic
    fun child(selectors: List<Selector>): Segment = ChildSegment(selectors)

    /**
     * Creates a new [ChildSegment] with the given selectors.
     *
     * @param selector the first selector
     * @param more additional selectors, if any
     * @return the child segment
     *
     * @see ChildSegment
     * @see Selector
     */
    @JvmStatic
    fun child(
      selector: Selector,
      vararg more: Selector,
    ): Segment = ChildSegment(selector, *more)

    /**
     * Creates a new [DescendantSegment] with the given selectors.
     *
     * @param selectors the selectors to use
     * @return the descendant segment
     *
     * @see DescendantSegment
     * @see Selector
     */
    @JvmStatic
    fun descendant(selectors: List<Selector>): Segment = DescendantSegment(selectors)

    /**
     * Creates a new [DescendantSegment] with the given selectors.
     *
     * @param selector the first selector
     * @param more additional selectors, if any
     * @return the descendant segment
     *
     * @see DescendantSegment
     * @see Selector
     */
    @JvmStatic
    fun descendant(
      selector: Selector,
      vararg more: Selector,
    ): Segment = DescendantSegment(selector, *more)
  }
}
