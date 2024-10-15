package com.kobil.vertx.jsonpath

/**
 * A base interface for JSON Path selectors.
 */
sealed interface Selector {
  /**
   * A selector matching fields with a specific name.
   *
   * @param name the field name to select
   */
  data class Name(
    val name: String,
  ) : Selector {
    /**
     * Serializes this selector to the corresponding JSON path representation
     */
    override fun toString(): String = "'$name'"
  }

  /**
   * A selector matching everything.
   */
  data object Wildcard : Selector {
    /**
     * Serializes this selector to the corresponding JSON path representation
     */
    override fun toString(): String = "*"
  }

  /**
   * A selector matching items with a specific index.
   *
   * @param index the index to select
   */
  data class Index(
    val index: Int,
  ) : Selector {
    /**
     * Serializes this selector to the corresponding JSON path representation
     */
    override fun toString(): String = "$index"
  }

  /**
   * A selector matching a range of indices between [first] (inclusive) and [last] (exclusive) with
   * a step size of [step]. For example, if `0 < first < last` and `step > 0`, then this selector
   * will select `first + k * step < last` for all values of `k >= 0` satisfying the inequality.
   *
   * The [first] and [last] indices may be negative, which refers to the "`k`th-last" element of an
   * array. For example, `-1` refers to the last element, `-2` refers to the second last element,
   * and so on. The [step] may be negative to return elements in reverse order, if the [first]
   * element is at a higher index than [last].
   *
   * Each component may be `null`, which makes it use a default value:
   * - [first]: the first element of the array in iteration order (if [step] is negative, this is
   *   the _last_ element of the array)
   * - [last]: the index _after the last_ element of the array in iteration order (if [step] is
   *   negative, this is the _first_ element of the array)
   * - [step]: Default 1
   *
   * @param first the first index (inclusive) of the slice, may be null
   * @param last the last index (exclusive) of the slice, may be null
   * @param step the step size of the slice, may be null
   */
  data class Slice(
    val first: Int?,
    val last: Int?,
    val step: Int?,
  ) : Selector {
    /**
     * Serializes this selector to the corresponding JSON path representation
     */
    override fun toString(): String =
      (first?.toString() ?: "") + ":" + (last?.toString() ?: "") + (step?.let { ":$it" } ?: "")
  }

  /**
   * A selector matching nodes for which the [filter] evaluates to `true`.
   *
   * @param filter the filter to apply to the candidates
   */
  data class Filter(
    val filter: FilterExpression,
  ) : Selector {
    /**
     * Serializes this selector to the corresponding JSON path representation
     */
    override fun toString(): String = "?$filter"
  }

  /**
   * Contains convenience factory methods
   */
  companion object {
    /**
     * A selector matching everything. This is for Java users, in Kotlin it is equivalent to
     * using [Wildcard].
     */
    @JvmField
    val WILDCARD: Selector = Wildcard

    /**
     * Constructor taking a field name and returning a [Name] selector.
     *
     * @param name field name to select
     * @return an equivalent [Name] selector
     *
     * @see Name
     */
    operator fun invoke(name: String): Selector = Name(name)

    /**
     * Factory taking a field name and returning a [Name] selector.
     *
     * @param name field name to select
     * @return an equivalent [Name] selector
     *
     * @see Name
     */
    @JvmStatic
    fun name(name: String): Selector = Selector(name)

    /**
     * Constructor taking an index and returning an [Index] selector.
     *
     * @param index index to select
     * @return an equivalent [Index] selector
     *
     * @see Index
     */
    operator fun invoke(index: Int): Selector = Index(index)

    /**
     * Factory taking an index and returning an [Index] selector.
     *
     * @param index index to select
     * @return an equivalent [Index] selector
     *
     * @see Index
     */
    @JvmStatic
    fun index(index: Int): Selector = Selector(index)

    /**
     * Constructor taking an [IntProgression] and returning a [Slice] selector selecting the indices
     * contained in the progression.
     *
     * @param slice indices to select
     * @return an equivalent [Slice] selector
     *
     * @see Slice
     */
    operator fun invoke(slice: IntProgression): Selector =
      if (slice.step >= 0) {
        Slice(slice.first, slice.last + 1, slice.step)
      } else {
        Slice(
          slice.first,
          if (slice.last != 0) slice.last - 1 else null,
          slice.step,
        )
      }

    /**
     * Factory taking an [IntProgression] and returning a [Slice] selector selecting the indices
     * contained in the progression.
     *
     * @param slice indices to select
     * @return an equivalent [Slice] selector
     *
     * @see Slice
     */
    @JvmStatic
    fun slice(slice: IntProgression): Selector = Selector(slice)

    /**
     * Constructor taking the [first], [last] and [step] parameters and returning a [Slice] selector
     * with the same parameters.
     *
     * @param firstInclusive the first index to include
     * @param lastExclusive the first index to exclude
     * @param step the step size
     * @return an equivalent [Slice] selector
     *
     * @see Slice
     */
    operator fun invoke(
      firstInclusive: Int?,
      lastExclusive: Int?,
      step: Int? = null,
    ): Selector = Slice(firstInclusive, lastExclusive, step)

    /**
     * Factory taking the [first], [last] and [step] parameters and returning a [Slice] selector
     * with the same parameters.
     *
     * @param firstInclusive the first index to include
     * @param lastExclusive the first index to exclude
     * @param step the step size
     * @return an equivalent [Slice] selector
     *
     * @see Slice
     */
    @JvmStatic
    @JvmOverloads
    fun slice(
      firstInclusive: Int?,
      lastExclusive: Int?,
      step: Int? = null,
    ): Selector = Selector(firstInclusive, lastExclusive, step)

    /**
     * Constructor taking a filter expression and returning a [Filter] selector.
     *
     * @param filter the filter expression to apply
     * @return an equivalent [Filter] selector
     *
     * @see Filter
     */
    operator fun invoke(filter: FilterExpression): Selector = Filter(filter)

    /**
     * Factory taking a filter expression and returning a [Filter] selector.
     *
     * @param filter the filter expression to apply
     * @return an equivalent [Filter] selector
     *
     * @see Filter
     */
    @JvmStatic
    fun filter(filter: FilterExpression): Selector = Selector(filter)
  }
}
