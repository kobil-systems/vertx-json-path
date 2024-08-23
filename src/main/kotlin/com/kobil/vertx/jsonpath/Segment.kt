package com.kobil.vertx.jsonpath

sealed interface Segment {
  val selectors: List<Selector>
  val isSingular: Boolean

  data class ChildSegment(
    override val selectors: List<Selector>,
  ) : Segment {
    constructor(selector: Selector, vararg more: Selector) : this(listOf(selector, *more))

    constructor(name: String, vararg more: String) : this(
      Selector(name),
      *more.map(Selector::invoke).toTypedArray(),
    )

    constructor(index: Int, vararg more: Int) : this(
      Selector(index),
      *more.map(Selector::invoke).toTypedArray(),
    )

    override val isSingular: Boolean
      get() =
        selectors.size == 1 &&
          (selectors.first() is Selector.Name || selectors.first() is Selector.Index)
  }

  data class DescendantSegment(
    override val selectors: List<Selector>,
  ) : Segment {
    constructor(selector: Selector, vararg more: Selector) : this(listOf(selector, *more))

    constructor(name: String, vararg more: String) : this(
      Selector(name),
      *more.map(Selector::invoke).toTypedArray(),
    )

    constructor(index: Int, vararg more: Int) : this(
      Selector(index),
      *more.map(Selector::invoke).toTypedArray(),
    )

    override val isSingular: Boolean
      get() = false
  }
}
