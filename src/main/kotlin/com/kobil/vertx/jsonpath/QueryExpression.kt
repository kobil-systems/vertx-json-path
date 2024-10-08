package com.kobil.vertx.jsonpath

sealed class QueryExpression<T : QueryExpression<T>> :
  NodeListExpression,
  JsonPathQuery<T> {
  val isSingular: Boolean
    get() = segments.all { it.isSingular }

  fun count(): ComparableExpression = FunctionExpression.Count(this)

  fun value(): ComparableExpression = FunctionExpression.Value(this)

  data class Relative
    @JvmOverloads
    constructor(
      override val segments: List<Segment> = listOf(),
    ) : QueryExpression<Relative>() {
      constructor(firstSegment: Segment, vararg more: Segment) : this(listOf(firstSegment, *more))

      override operator fun plus(segment: Segment): Relative = copy(segments = segments + segment)

      override operator fun plus(segments: Iterable<Segment>): Relative =
        copy(segments = this.segments + segments)

      override fun toString(): String = segments.joinToString("", prefix = "@")
    }

  data class Absolute
    @JvmOverloads
    constructor(
      override val segments: List<Segment> = listOf(),
    ) : QueryExpression<Absolute>() {
      constructor(firstSegment: Segment, vararg more: Segment) : this(listOf(firstSegment, *more))

      override operator fun plus(segment: Segment): Absolute = copy(segments = segments + segment)

      override operator fun plus(segments: Iterable<Segment>): Absolute =
        copy(segments = this.segments + segments)

      override fun toString(): String = segments.joinToString("", prefix = "$")
    }

  companion object {
    @JvmStatic
    @JvmOverloads
    fun relative(segments: List<Segment> = listOf()): Relative = Relative(segments)

    @JvmStatic
    fun relative(
      firstSegment: Segment,
      vararg more: Segment,
    ): Relative = Relative(firstSegment, *more)

    @JvmStatic
    @JvmOverloads
    fun absolute(segments: List<Segment> = listOf()): Absolute = Absolute(segments)

    @JvmStatic
    fun absolute(
      firstSegment: Segment,
      vararg more: Segment,
    ): Absolute = Absolute(firstSegment, *more)
  }
}
