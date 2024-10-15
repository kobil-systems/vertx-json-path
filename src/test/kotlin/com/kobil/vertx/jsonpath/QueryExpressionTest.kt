package com.kobil.vertx.jsonpath

import arrow.core.toNonEmptyListOrNull
import com.kobil.vertx.jsonpath.QueryExpression.Absolute
import com.kobil.vertx.jsonpath.QueryExpression.Relative
import com.kobil.vertx.jsonpath.Segment.ChildSegment
import com.kobil.vertx.jsonpath.Segment.DescendantSegment
import com.kobil.vertx.jsonpath.testing.normalizedJsonPath
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll

class QueryExpressionTest :
  ShouldSpec({
    context("The count method") {
      should("return a count function called on the query expression") {
        checkAll(Arb.normalizedJsonPath()) { (segments) ->
          val abs = Absolute(segments)
          val rel = Relative(segments)

          abs.count() shouldBe FunctionExpression.Count(abs)
          rel.count() shouldBe FunctionExpression.Count(rel)
        }
      }
    }

    context("The value method") {
      should("return a value function called on the query expression") {
        checkAll(Arb.normalizedJsonPath()) { (segments) ->
          val abs = Absolute(segments)
          val rel = Relative(segments)

          abs.value() shouldBe FunctionExpression.Value(abs)
          rel.value() shouldBe FunctionExpression.Value(rel)
        }
      }
    }

    context("The plus operator") {
      context("called on an absolute query") {
        context("specifying a single segment") {
          should("append the segment") {
            val n = Selector.name("a")
            val i = Selector.index(1)
            val s = Selector.slice(1, -1, 2)
            val w = Selector.Wildcard
            val f = Selector.filter(QueryExpression.absolute().exists())

            (Absolute() + ChildSegment(n)).segments shouldBe
              listOf(
                ChildSegment(n),
              )

            (Absolute() + ChildSegment(i, f)).segments shouldBe
              listOf(
                ChildSegment(i, f),
              )

            (
              Absolute() + DescendantSegment(n) + ChildSegment(w) +
                DescendantSegment(
                  s,
                  f,
                )
            ).segments shouldBe
              listOf(
                DescendantSegment(n),
                ChildSegment(w),
                DescendantSegment(s, f),
              )
          }
        }

        context("specifying iterable segments") {
          should("append all segments") {
            checkAll(Arb.normalizedJsonPath()) { (segments) ->
              val n = Selector.name("a")

              (Absolute() + segments).segments shouldBe segments
              (Absolute() + ChildSegment(n) + segments).segments shouldBe
                listOf(ChildSegment(n)) + segments
            }
          }
        }
      }

      context("called on a relative query") {
        context("specifying a single segment") {
          should("append the segment") {
            val n = Selector.name("a")
            val i = Selector.index(1)
            val s = Selector.slice(1, -1, 2)
            val w = Selector.Wildcard
            val f = Selector.filter(QueryExpression.relative().exists())

            (Relative() + ChildSegment(n)).segments shouldBe
              listOf(
                ChildSegment(n),
              )

            (Relative() + ChildSegment(i, f)).segments shouldBe
              listOf(
                ChildSegment(i, f),
              )

            (
              Relative() + DescendantSegment(n) + ChildSegment(w) +
                DescendantSegment(
                  s,
                  f,
                )
            ).segments shouldBe
              listOf(
                DescendantSegment(n),
                ChildSegment(w),
                DescendantSegment(s, f),
              )
          }
        }

        context("specifying iterable segments") {
          should("append all segments") {
            checkAll(Arb.normalizedJsonPath()) { (segments) ->
              val n = Selector.name("a")

              (Relative() + segments).segments shouldBe segments
              (Relative() + ChildSegment(n) + segments).segments shouldBe
                listOf(ChildSegment(n)) + segments
            }
          }
        }
      }
    }

    context("The toString function") {
      context("called on an absolute query") {
        should("return the serialized segments, prefixed by $") {
          checkAll(Arb.normalizedJsonPath()) { (segments) ->
            Absolute(segments).toString() shouldBe "$" + segments.joinToString("")
          }
        }
      }

      context("called on a relative query") {
        should("return the serialized segments, prefixed by @") {
          checkAll(Arb.normalizedJsonPath()) { (segments) ->
            Relative(segments).toString() shouldBe "@" + segments.joinToString("")
          }
        }
      }
    }

    context("The static absolute method") {
      context("called without an argument") {
        should("return an empty absolute query") {
          QueryExpression.absolute() shouldBe Absolute(listOf())
        }
      }

      context("applied to a list of segments") {
        should("return an absolute query with the same list of segments") {
          checkAll(Arb.normalizedJsonPath()) { (segments) ->
            QueryExpression.absolute(segments) shouldBe Absolute(segments)
          }
        }
      }

      context("applied to vararg segments") {
        should("return an absolute query with the same segments") {
          checkAll(
            Arb
              .normalizedJsonPath()
              .map { it.segments.toNonEmptyListOrNull() }
              .filter { it != null },
          ) { segments ->
            QueryExpression.absolute(
              segments!!.head,
              *segments.tail.toTypedArray(),
            ) shouldBe Absolute(segments)
          }
        }
      }
    }

    context("The static relative method") {
      context("called without an argument") {
        should("return an empty relative query") {
          QueryExpression.relative() shouldBe Relative(listOf())
        }
      }

      context("applied to a list of segments") {
        should("return a relative query with the same list of segments") {
          checkAll(Arb.normalizedJsonPath()) { (segments) ->
            QueryExpression.relative(segments) shouldBe Relative(segments)
          }
        }
      }

      context("applied to vararg segments") {
        should("return a relative query with the same segments") {
          checkAll(
            Arb
              .normalizedJsonPath()
              .map { it.segments.toNonEmptyListOrNull() }
              .filter { it != null },
          ) { segments ->
            QueryExpression.relative(
              segments!!.head,
              *segments.tail.toTypedArray(),
            ) shouldBe Relative(segments)
          }
        }
      }
    }
  })
