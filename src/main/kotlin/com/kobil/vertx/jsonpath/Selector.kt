package com.kobil.vertx.jsonpath

sealed interface Selector {
  data class Name(
    val name: String,
  ) : Selector

  data object Wildcard : Selector

  data class Index(
    val index: Int,
  ) : Selector

  data class Slice(
    val first: Int?,
    val last: Int?,
    val step: Int?,
  ) : Selector

  data class Filter(
    val filter: FilterExpression,
  ) : Selector

  companion object {
    operator fun invoke(name: String): Selector = Name(name)

    operator fun invoke(index: Int): Selector = Index(index)

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

    operator fun invoke(
      first: Int?,
      lastExclusive: Int?,
      step: Int? = null,
    ): Selector = Slice(first, lastExclusive, step)

    operator fun invoke(filter: FilterExpression): Selector = Filter(filter)
  }
}
