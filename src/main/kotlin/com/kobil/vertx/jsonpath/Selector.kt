package com.kobil.vertx.jsonpath

sealed interface Selector {
  data class Name(
    val name: String,
  ) : Selector {
    override fun toString(): String = "'$name'"
  }

  data object Wildcard : Selector {
    override fun toString(): String = "*"
  }

  data class Index(
    val index: Int,
  ) : Selector {
    override fun toString(): String = "$index"
  }

  data class Slice(
    val first: Int?,
    val last: Int?,
    val step: Int?,
  ) : Selector {
    override fun toString(): String =
      (first?.toString() ?: "") + ":" + (last?.toString() ?: "") + (step?.let { ":$it" } ?: "")
  }

  data class Filter(
    val filter: FilterExpression,
  ) : Selector {
    override fun toString(): String = "?$filter"
  }

  companion object {
    @JvmField
    val WILDCARD: Selector = Wildcard

    operator fun invoke(name: String): Selector = Name(name)

    @JvmStatic
    fun name(name: String): Selector = Selector(name)

    operator fun invoke(index: Int): Selector = Index(index)

    @JvmStatic
    fun index(index: Int): Selector = Selector(index)

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

    @JvmStatic
    fun slice(slice: IntProgression): Selector = Selector(slice)

    operator fun invoke(
      firstInclusive: Int?,
      lastExclusive: Int?,
      step: Int? = null,
    ): Selector = Slice(firstInclusive, lastExclusive, step)

    @JvmStatic
    @JvmOverloads
    fun slice(
      firstInclusive: Int?,
      lastExclusive: Int?,
      step: Int? = null,
    ): Selector = Selector(firstInclusive, lastExclusive, step)

    operator fun invoke(filter: FilterExpression): Selector = Filter(filter)

    @JvmStatic
    fun filter(filter: FilterExpression): Selector = Selector(filter)
  }
}
