package com.kobil.vertx.jsonpath

sealed interface Segment {
  val selectors: List<Selector>
  val isSingular: Boolean

  data class ChildSegment(
    override val selectors: List<Selector>,
  ) : Segment {
    init {
      require(selectors.isNotEmpty()) { "A child segment without selectors is not allowed" }
    }

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

    override fun toString(): String = selectors.joinToString(",", prefix = "[", postfix = "]")
  }

  data class DescendantSegment(
    override val selectors: List<Selector>,
  ) : Segment {
    init {
      require(selectors.isNotEmpty()) { "A descendant segment without selectors is not allowed" }
    }

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

    override fun toString(): String = selectors.joinToString(",", prefix = "..[", postfix = "]")
  }

  companion object {
    @JvmStatic
    fun child(selectors: List<Selector>): Segment = ChildSegment(selectors)

    @JvmStatic
    fun child(
      selector: Selector,
      vararg more: Selector,
    ): Segment = ChildSegment(selector, *more)

    @JvmStatic
    fun descendant(selectors: List<Selector>): Segment = DescendantSegment(selectors)

    @JvmStatic
    fun descendant(
      selector: Selector,
      vararg more: Selector,
    ): Segment = DescendantSegment(selector, *more)
  }
}
