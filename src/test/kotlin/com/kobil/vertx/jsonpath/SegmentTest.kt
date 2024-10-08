package com.kobil.vertx.jsonpath

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe

class SegmentTest :
  ShouldSpec({
    context("The ChildSegment") {
      context("primary constructor") {
        should("throw when called with an empty list") {
          shouldThrow<IllegalArgumentException> {
            Segment.ChildSegment(listOf())
          }
        }
      }

      context("toString method") {
        should(
          "return a string of all selectors, joined by commas, enclosed in square brackets",
        ) {
          Segment.ChildSegment("a", "b", "c").toString() shouldBe "['a','b','c']"
          Segment.ChildSegment("a").toString() shouldBe "['a']"
          Segment.ChildSegment(1).toString() shouldBe "[1]"
          Segment.ChildSegment(Selector.Wildcard).toString() shouldBe "[*]"
          Segment
            .ChildSegment(
              Selector.name("a"),
              Selector.Wildcard,
              Selector.index(1),
            ).toString() shouldBe "['a',*,1]"
        }
      }
    }

    context("The DescendantSegment") {
      context("primary constructor") {
        should("throw when called with an empty list") {
          shouldThrow<IllegalArgumentException> {
            Segment.DescendantSegment(listOf())
          }
        }
      }

      context("toString method") {
        should(
          "return a string of all selectors, separated by commas, enclosed in square brackets, preceded by ..",
        ) {
          Segment.DescendantSegment("a", "b", "c").toString() shouldBe "..['a','b','c']"
          Segment.DescendantSegment("a").toString() shouldBe "..['a']"
          Segment.DescendantSegment(1).toString() shouldBe "..[1]"
          Segment.DescendantSegment(Selector.Wildcard).toString() shouldBe "..[*]"
          Segment
            .DescendantSegment(Selector.name("a"), Selector.Wildcard, Selector.index(1))
            .toString() shouldBe "..['a',*,1]"
        }
      }
    }

    context("The child static function") {
      context("taking a list of selectors") {
        should("throw on an empty list") {
          shouldThrow<IllegalArgumentException> {
            Segment.child(listOf())
          }
        }

        should("return a child segment with the specified list of selectors") {
          val sel =
            listOf(
              Selector.name("a"),
              Selector.index(1),
              Selector.slice(1, -1, 2),
              Selector.Wildcard,
              Selector.Filter(QueryExpression.absolute().exists()),
            )

          Segment.child(sel) shouldBe Segment.ChildSegment(sel)
        }
      }

      context("taking varargs") {
        should("return a child segment with the specified selectors") {
          val n = Selector.name("a")
          val i = Selector.index(1)
          val s = Selector.slice(1, -1, 2)
          val w = Selector.Wildcard
          val f = Selector.Filter(QueryExpression.absolute().exists())

          Segment.child(n, i, s, w, f) shouldBe Segment.ChildSegment(listOf(n, i, s, w, f))
        }
      }
    }

    context("The descendant static function") {
      context("taking a list of selectors") {
        should("throw on an empty list") {
          shouldThrow<IllegalArgumentException> {
            Segment.descendant(listOf())
          }
        }

        should("return a descendant segment with the specified list of selectors") {
          val sel =
            listOf(
              Selector.name("a"),
              Selector.index(1),
              Selector.slice(1, -1, 2),
              Selector.Wildcard,
              Selector.filter(QueryExpression.absolute().exists()),
            )

          Segment.descendant(sel) shouldBe Segment.DescendantSegment(sel)
        }
      }

      context("taking varargs") {
        should("return a descendant segment with the specified selectors") {
          val n = Selector.name("a")
          val i = Selector.index(1)
          val s = Selector.slice(1, -1, 2)
          val w = Selector.Wildcard
          val f = Selector.filter(QueryExpression.absolute().exists())

          Segment.descendant(n, i, s, w, f) shouldBe
            Segment.DescendantSegment(listOf(n, i, s, w, f))
        }
      }
    }
  })
