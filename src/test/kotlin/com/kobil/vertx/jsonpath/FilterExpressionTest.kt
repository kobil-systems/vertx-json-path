package com.kobil.vertx.jsonpath

import com.kobil.vertx.jsonpath.compiler.Token
import com.kobil.vertx.jsonpath.error.JsonPathError
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.vertx.kotlin.core.json.jsonArrayOf
import io.vertx.kotlin.core.json.jsonObjectOf

class FilterExpressionTest :
  ShouldSpec({
    context("Invalid filter expressions") {
      context("with a leading question mark") {
        should("result in an error when compiled") {
          val unexpectedQuestion =
            JsonPathError.UnexpectedToken(
              Token.QuestionMark(1U, 1U),
              "comparable expression",
            )

          // Leading ? must be omitted
          FilterExpression.compile("?@") shouldBeLeft unexpectedQuestion
          FilterExpression.compile("?@.abc") shouldBeLeft unexpectedQuestion
          FilterExpression.compile("?!@") shouldBeLeft unexpectedQuestion
          FilterExpression.compile("?!@.abc") shouldBeLeft unexpectedQuestion
          FilterExpression.compile("?@ == 'x'") shouldBeLeft unexpectedQuestion
          FilterExpression.compile("?@.abc < 1U") shouldBeLeft unexpectedQuestion
          FilterExpression.compile("?@['xyz'] >= 'a'") shouldBeLeft unexpectedQuestion
          FilterExpression.compile("?@.abc != 2U") shouldBeLeft unexpectedQuestion
        }
      }

      context("with a non-singular query as the operand of a comparison expression") {
        should("result in an error when compiled") {
          FilterExpression.compile("@..a == 2") shouldBeLeft
            JsonPathError.MustBeSingularQuery(
              1U,
              1U,
            )

          FilterExpression.compile("2 == @..a") shouldBeLeft
            JsonPathError.MustBeSingularQuery(
              1U,
              6U,
            )

          FilterExpression.compile("@.a[1, 2] == 2") shouldBeLeft
            JsonPathError.MustBeSingularQuery(
              1U,
              1U,
            )

          FilterExpression.compile("2 == @.a[1, 2]") shouldBeLeft
            JsonPathError.MustBeSingularQuery(
              1U,
              6U,
            )

          FilterExpression.compile("@.a['b', 'c'] == 2") shouldBeLeft
            JsonPathError.MustBeSingularQuery(
              1U,
              1U,
            )

          FilterExpression.compile("2 == @.a['b', 'c']") shouldBeLeft
            JsonPathError.MustBeSingularQuery(
              1U,
              6U,
            )

          FilterExpression.compile("@.a[1:] == 2") shouldBeLeft
            JsonPathError.MustBeSingularQuery(
              1U,
              1U,
            )

          FilterExpression.compile("2 == @.a[1:]") shouldBeLeft
            JsonPathError.MustBeSingularQuery(
              1U,
              6U,
            )

          FilterExpression.compile("@.a[?@.b == 1] == 2") shouldBeLeft
            JsonPathError.MustBeSingularQuery(
              1U,
              1U,
            )

          FilterExpression.compile("2 == @.a[?@.b == 1]") shouldBeLeft
            JsonPathError.MustBeSingularQuery(
              1U,
              6U,
            )
        }
      }
    }

    context("Valid filter expressions") {
      context("using a simple existence check") {
        val at = Token.At(1U, 1U)

        val hasA = "@.a"
        val hasB = "@['b']"
        val has1 = "@[1]"
        val hasNestedA = "@..a"
        val hasNestedB = "@..['b']"
        val hasNested1 = "@..[1]"

        should("compile successfully") {
          FilterExpression.compile(hasA) shouldBeRight
            FilterExpression.Existence(
              QueryExpression.Relative(listOf(Segment.ChildSegment("a")), at),
            )

          FilterExpression.compile(hasB) shouldBeRight
            FilterExpression.Existence(
              QueryExpression.Relative(listOf(Segment.ChildSegment("b")), at),
            )

          FilterExpression.compile(has1) shouldBeRight
            FilterExpression.Existence(
              QueryExpression.Relative(listOf(Segment.ChildSegment(1)), at),
            )
          FilterExpression.compile(hasNestedA) shouldBeRight
            FilterExpression.Existence(
              QueryExpression.Relative(listOf(Segment.DescendantSegment("a")), at),
            )

          FilterExpression.compile(hasNestedB) shouldBeRight
            FilterExpression.Existence(
              QueryExpression.Relative(listOf(Segment.DescendantSegment("b")), at),
            )

          FilterExpression.compile(hasNested1) shouldBeRight
            FilterExpression.Existence(
              QueryExpression.Relative(listOf(Segment.DescendantSegment(1)), at),
            )
        }

        context("of a child segment") {
          should("return true for inputs if, and only if, they contain the required element") {
            val obj1 = jsonObjectOf("a" to 1)
            val obj2 = jsonObjectOf("b" to 2)
            val obj3 = jsonObjectOf("a" to 1, "b" to 2)
            val objANull = jsonObjectOf("a" to null)
            val objName1 = jsonObjectOf("1" to true)

            val arr1 = jsonArrayOf("a")
            val arr2 = jsonArrayOf("a", "b")
            val arr3 = jsonArrayOf("a", "b", "c", "d")
            val arr4 = jsonArrayOf(1)

            FilterExpression.compile(hasA).shouldBeRight().also {
              it.match(obj1).shouldBeTrue()
              it.match(obj2).shouldBeFalse()
              it.match(obj3).shouldBeTrue()
              it.match(objANull).shouldBeTrue()
              it.match(arr1).shouldBeFalse()
              it.match(arr2).shouldBeFalse()
              it.match(arr3).shouldBeFalse()
              it.match(arr4).shouldBeFalse()
            }

            FilterExpression.compile(hasB).shouldBeRight().also {
              it.match(obj1).shouldBeFalse()
              it.match(obj2).shouldBeTrue()
              it.match(obj3).shouldBeTrue()
              it.match(objANull).shouldBeFalse()
              it.match(arr1).shouldBeFalse()
              it.match(arr2).shouldBeFalse()
              it.match(arr3).shouldBeFalse()
              it.match(arr4).shouldBeFalse()
            }

            FilterExpression.compile(has1).shouldBeRight().also {
              it.match(obj1).shouldBeFalse()
              it.match(obj2).shouldBeFalse()
              it.match(obj3).shouldBeFalse()
              it.match(objANull).shouldBeFalse()
              it.match(objName1).shouldBeFalse()
              it.match(arr1).shouldBeFalse()
              it.match(arr2).shouldBeTrue()
              it.match(arr3).shouldBeTrue()
              it.match(arr4).shouldBeFalse()
            }
          }
        }

        context("of a descendant segment") {
          should("return true for inputs if, and only if, they contain the required element") {
            val obj1 = jsonObjectOf("a" to 1)
            val obj2 = jsonObjectOf("b" to jsonObjectOf("a" to 1))
            val obj3 = jsonObjectOf("a" to 1, "b" to jsonArrayOf(1))
            val obj4 = jsonObjectOf("a" to jsonArrayOf(1, 2, 3))
            val obj5 =
              jsonObjectOf(
                "c" to
                  jsonArrayOf(
                    jsonObjectOf("a" to true),
                    jsonObjectOf("b" to false),
                  ),
              )
            val obj6 = jsonObjectOf("b" to jsonArrayOf(1, 2), "c" to jsonObjectOf("b" to 1))

            val arr1 = jsonArrayOf("a")
            val arr2 = jsonArrayOf("a", "b", jsonObjectOf("a" to 1))
            val arr3 = jsonArrayOf("a", "b", "c", "d")
            val arr4 = jsonArrayOf(jsonArrayOf("a", "b"))

            FilterExpression.compile(hasNestedA).shouldBeRight().also {
              it.match(obj1).shouldBeTrue()
              it.match(obj2).shouldBeTrue()
              it.match(obj3).shouldBeTrue()
              it.match(obj4).shouldBeTrue()
              it.match(obj5).shouldBeTrue()
              it.match(obj6).shouldBeFalse()
              it.match(arr1).shouldBeFalse()
              it.match(arr2).shouldBeTrue()
              it.match(arr3).shouldBeFalse()
              it.match(arr4).shouldBeFalse()
            }

            FilterExpression.compile(hasNestedB).shouldBeRight().also {
              it.match(obj1).shouldBeFalse()
              it.match(obj2).shouldBeTrue()
              it.match(obj3).shouldBeTrue()
              it.match(obj4).shouldBeFalse()
              it.match(obj5).shouldBeTrue()
              it.match(obj6).shouldBeTrue()
              it.match(arr1).shouldBeFalse()
              it.match(arr2).shouldBeFalse()
              it.match(arr3).shouldBeFalse()
              it.match(arr4).shouldBeFalse()
            }

            FilterExpression.compile(hasNested1).shouldBeRight().also {
              it.match(obj1).shouldBeFalse()
              it.match(obj2).shouldBeFalse()
              it.match(obj3).shouldBeFalse()
              it.match(obj4).shouldBeTrue()
              it.match(obj5).shouldBeTrue()
              it.match(obj6).shouldBeTrue()
              it.match(arr1).shouldBeFalse()
              it.match(arr2).shouldBeTrue()
              it.match(arr3).shouldBeTrue()
              it.match(arr4).shouldBeTrue()
            }
          }
        }
      }
    }
  })
