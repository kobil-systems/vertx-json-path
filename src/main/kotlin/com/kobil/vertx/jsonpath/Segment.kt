package com.kobil.vertx.jsonpath

sealed interface Segment {
  val selectors: List<Selector>
  val isSingular: Boolean

  data class ChildSegment(
    override val selectors: List<Selector>,
  ) : Segment {
    override val isSingular: Boolean
      get() =
        selectors.size == 1 &&
          (selectors.first() is Selector.Name || selectors.first() is Selector.Index)
  }

  data class DescendantSegment(
    override val selectors: List<Selector>,
  ) : Segment {
    override val isSingular: Boolean
      get() = false
  }
}
