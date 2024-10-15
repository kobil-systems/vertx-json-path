package com.kobil.vertx.jsonpath

import arrow.core.nonEmptyListOf
import com.kobil.vertx.jsonpath.ComparableExpression.Literal
import com.kobil.vertx.jsonpath.FilterExpression.Comparison
import com.kobil.vertx.jsonpath.FilterExpression.Not
import com.kobil.vertx.jsonpath.FilterExpression.Or
import com.kobil.vertx.jsonpath.FilterExpression.Test
import com.kobil.vertx.jsonpath.QueryExpression.Relative
import com.kobil.vertx.jsonpath.Segment.ChildSegment
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

class SelectorTest :
  ShouldSpec({
    context("the toString method") {
      context("of a name selector") {
        should("return the name enclosed in single quotes") {
          checkAll(Arb.string()) {
            Selector.Name(it).toString() shouldBe "'$it'"
          }
        }
      }

      context("of an index selector") {
        should("return the index") {
          checkAll(Arb.int()) {
            Selector.Index(it).toString() shouldBe "$it"
          }
        }
      }

      context("of the wildcard selector") {
        should("return a literal *") {
          Selector.Wildcard.toString() shouldBe "*"
        }
      }

      context("of a slice selector") {
        should("return the three components separated by colons") {
          checkAll(Arb.int(), Arb.int(), Arb.int()) { first, last, step ->
            Selector.Slice(first, last, step).toString() shouldBe "$first:$last:$step"
          }
        }

        should("omit the step component when it is null") {
          checkAll(Arb.int(), Arb.int()) { first, last ->
            Selector.Slice(first, last, null).toString() shouldBe "$first:$last"
          }
        }

        should("have an empty first component when it is null") {
          checkAll(Arb.int(), Arb.int()) { last, step ->
            Selector.Slice(null, last, step).toString() shouldBe ":$last:$step"
          }
        }

        should("have an empty last component when it is null") {
          checkAll(Arb.int(), Arb.int()) { first, step ->
            Selector.Slice(first, null, step).toString() shouldBe "$first::$step"
          }
        }

        should("have empty first and last components when they are both null") {
          checkAll(Arb.int()) { step ->
            Selector.Slice(null, null, step).toString() shouldBe "::$step"
          }
        }

        should("return a single colon if all components are null") {
          Selector.Slice(null, null, null).toString() shouldBe ":"
        }
      }

      context("of a filter selector") {
        should("return the serialized filter expression preceded by a question mark") {
          Selector
            .Filter(
              Test(
                Relative(ChildSegment(Selector.Name("a"))),
              ),
            ).toString() shouldBe "?@['a']"
        }
      }
    }

    context("the invoke operator") {
      context("called on a string") {
        should("return a name selector with the given field name") {
          checkAll(Arb.string()) {
            Selector(it) shouldBe Selector.Name(it)
          }
        }
      }

      context("called on an integer") {
        should("return an index selector with the given index") {
          checkAll(Arb.int()) {
            Selector(it) shouldBe Selector.Index(it)
          }
        }
      }

      context("called on an IntProgression") {
        should("return an equivalent slice selector") {
          Selector(1..2) shouldBe Selector.Slice(1, 3, 1)
          Selector(1..<5) shouldBe Selector.Slice(1, 5, 1)
          Selector(1..10 step 2) shouldBe Selector.Slice(1, 10, 2)
          Selector(10 downTo 1) shouldBe Selector.Slice(10, 0, -1)
          Selector(10 downTo 1 step 4) shouldBe Selector.Slice(10, 1, -4)
        }
      }

      context("called on three nullable integers") {
        should("return a slice selector with the same parameters") {
          checkAll(Arb.int(), Arb.int(), Arb.int()) { first, last, step ->
            Selector(first, last, step) shouldBe Selector.Slice(first, last, step)
          }
        }
      }

      context("called on two nullable integers") {
        should("return a slice selector with the same parameters and a null step") {
          checkAll(Arb.int(), Arb.int()) { first, last ->
            Selector(first, last) shouldBe Selector.Slice(first, last, null)
          }
        }
      }

      context("called on a filter expression") {
        should("return a filter selector with the given filter expression") {
          val expr1 = Test(Relative(ChildSegment("a")))
          val expr2 =
            Or(
              nonEmptyListOf(
                Comparison(
                  Comparison.Op.GREATER_EQ,
                  Relative(ChildSegment("a")),
                  Literal(1),
                ),
                Not(Test(Relative(ChildSegment("a")))),
              ),
            )

          Selector(expr1) shouldBe Selector.Filter(expr1)
          Selector(expr2) shouldBe Selector.Filter(expr2)
        }
      }
    }

    context("the name static function") {
      should("return a name selector with the given field name") {
        checkAll(Arb.string()) {
          Selector.name(it) shouldBe Selector.Name(it)
        }
      }
    }

    context("the index static function") {
      should("return an index selector with the given index") {
        checkAll(Arb.int()) {
          Selector.index(it) shouldBe Selector.Index(it)
        }
      }
    }

    context("the slice static function") {
      context("called on an IntProgression") {
        should("return an equivalent slice selector") {
          Selector.slice(1..2) shouldBe Selector.Slice(1, 3, 1)
          Selector.slice(1..<5) shouldBe Selector.Slice(1, 5, 1)
          Selector.slice(1..10 step 2) shouldBe Selector.Slice(1, 10, 2)
          Selector.slice(10 downTo 1) shouldBe Selector.Slice(10, 0, -1)
          Selector.slice(10 downTo 1 step 4) shouldBe Selector.Slice(10, 1, -4)
        }
      }

      context("called on three nullable integers") {
        should("return a slice selector with the same parameters") {
          checkAll(Arb.int(), Arb.int(), Arb.int()) { first, last, step ->
            Selector.slice(first, last, step) shouldBe Selector.Slice(first, last, step)
          }
        }
      }

      context("called on two nullable integers") {
        should("return a slice selector with the same parameters and a null step") {
          checkAll(Arb.int(), Arb.int()) { first, last ->
            Selector.slice(first, last) shouldBe Selector.Slice(first, last, null)
          }
        }
      }
    }

    context("the filter static function") {
      should("return a filter selector with the given filter expression") {
        val expr1 = Test(Relative(ChildSegment("a")))
        val expr2 =
          Or(
            nonEmptyListOf(
              Comparison(
                Comparison.Op.GREATER_EQ,
                Relative(ChildSegment("a")),
                Literal(1),
              ),
              Not(Test(Relative(ChildSegment("a")))),
            ),
          )

        Selector.filter(expr1) shouldBe Selector.Filter(expr1)
        Selector.filter(expr2) shouldBe Selector.Filter(expr2)
      }
    }
  })
