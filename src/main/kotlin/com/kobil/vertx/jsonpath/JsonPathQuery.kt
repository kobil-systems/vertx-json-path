package com.kobil.vertx.jsonpath

interface JsonPathQuery<T : JsonPathQuery<T>> {
  val segments: List<Segment>

  fun selectChildren(
    selector: Selector,
    vararg more: Selector,
  ): T = this + Segment.ChildSegment(selector, *more)

  fun selectDescendants(
    selector: Selector,
    vararg more: Selector,
  ): T = this + Segment.DescendantSegment(selector, *more)

  fun selectAllChildren(): T = selectChildren(Selector.WILDCARD)

  fun selectAllDescendants(): T = selectDescendants(Selector.WILDCARD)

  fun field(
    field: String,
    vararg more: String,
  ): T = this + Segment.ChildSegment(field, *more)

  fun index(
    index: Int,
    vararg more: Int,
  ): T = this + Segment.ChildSegment(index, *more)

  operator fun plus(segment: Segment): T

  operator fun plus(segments: Iterable<Segment>): T
}

operator fun <T : JsonPathQuery<T>> T.get(
  selector: Selector,
  vararg more: Selector,
): T = selectChildren(selector, *more)

operator fun <T : JsonPathQuery<T>> T.get(
  field: String,
  vararg more: String,
): T = field(field, *more)

operator fun <T : JsonPathQuery<T>> T.get(
  index: Int,
  vararg more: Int,
): T = index(index, *more)
