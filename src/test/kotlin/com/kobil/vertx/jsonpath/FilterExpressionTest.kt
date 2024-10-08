package com.kobil.vertx.jsonpath

import arrow.core.nonEmptyListOf
import com.kobil.vertx.jsonpath.ComparableExpression.Literal
import com.kobil.vertx.jsonpath.FilterExpression.And
import com.kobil.vertx.jsonpath.FilterExpression.Comparison
import com.kobil.vertx.jsonpath.FilterExpression.Comparison.Op
import com.kobil.vertx.jsonpath.FilterExpression.Existence
import com.kobil.vertx.jsonpath.FilterExpression.Match
import com.kobil.vertx.jsonpath.FilterExpression.Not
import com.kobil.vertx.jsonpath.FilterExpression.Or
import com.kobil.vertx.jsonpath.QueryExpression.Absolute
import com.kobil.vertx.jsonpath.QueryExpression.Relative
import com.kobil.vertx.jsonpath.compiler.Token
import com.kobil.vertx.jsonpath.error.JsonPathError
import com.kobil.vertx.jsonpath.testing.comparable
import com.kobil.vertx.jsonpath.testing.matchOperand
import com.kobil.vertx.jsonpath.testing.queryExpression
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.checkAll
import io.vertx.kotlin.core.json.jsonArrayOf
import io.vertx.kotlin.core.json.jsonObjectOf
import arrow.core.nonEmptyListOf as nel

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
        val hasA = "@.a"
        val hasB = "@['b']"
        val has1 = "@[1]"
        val hasNestedA = "@..a"
        val hasNestedB = "@..['b']"
        val hasNested1 = "@..[1]"

        should("compile successfully") {
          FilterExpression.compile(hasA) shouldBeRight
            Existence(
              Relative(listOf(Segment.ChildSegment("a"))),
            )

          FilterExpression.compile(hasB) shouldBeRight
            Existence(
              Relative(listOf(Segment.ChildSegment("b"))),
            )

          FilterExpression.compile(has1) shouldBeRight
            Existence(
              Relative(listOf(Segment.ChildSegment(1))),
            )
          FilterExpression.compile(hasNestedA) shouldBeRight
            Existence(
              Relative(listOf(Segment.DescendantSegment("a"))),
            )

          FilterExpression.compile(hasNestedB) shouldBeRight
            Existence(
              Relative(listOf(Segment.DescendantSegment("b"))),
            )

          FilterExpression.compile(hasNested1) shouldBeRight
            Existence(
              Relative(listOf(Segment.DescendantSegment(1))),
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
              it.test(obj1).shouldBeTrue()
              it.test(obj2).shouldBeFalse()
              it.test(obj3).shouldBeTrue()
              it.test(objANull).shouldBeTrue()
              it.test(arr1).shouldBeFalse()
              it.test(arr2).shouldBeFalse()
              it.test(arr3).shouldBeFalse()
              it.test(arr4).shouldBeFalse()
            }

            FilterExpression.compile(hasB).shouldBeRight().also {
              it.test(obj1).shouldBeFalse()
              it.test(obj2).shouldBeTrue()
              it.test(obj3).shouldBeTrue()
              it.test(objANull).shouldBeFalse()
              it.test(arr1).shouldBeFalse()
              it.test(arr2).shouldBeFalse()
              it.test(arr3).shouldBeFalse()
              it.test(arr4).shouldBeFalse()
            }

            FilterExpression.compile(has1).shouldBeRight().also {
              it.test(obj1).shouldBeFalse()
              it.test(obj2).shouldBeFalse()
              it.test(obj3).shouldBeFalse()
              it.test(objANull).shouldBeFalse()
              it.test(objName1).shouldBeFalse()
              it.test(arr1).shouldBeFalse()
              it.test(arr2).shouldBeTrue()
              it.test(arr3).shouldBeTrue()
              it.test(arr4).shouldBeFalse()
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
              it.test(obj1).shouldBeTrue()
              it.test(obj2).shouldBeTrue()
              it.test(obj3).shouldBeTrue()
              it.test(obj4).shouldBeTrue()
              it.test(obj5).shouldBeTrue()
              it.test(obj6).shouldBeFalse()
              it.test(arr1).shouldBeFalse()
              it.test(arr2).shouldBeTrue()
              it.test(arr3).shouldBeFalse()
              it.test(arr4).shouldBeFalse()
            }

            FilterExpression.compile(hasNestedB).shouldBeRight().also {
              it.test(obj1).shouldBeFalse()
              it.test(obj2).shouldBeTrue()
              it.test(obj3).shouldBeTrue()
              it.test(obj4).shouldBeFalse()
              it.test(obj5).shouldBeTrue()
              it.test(obj6).shouldBeTrue()
              it.test(arr1).shouldBeFalse()
              it.test(arr2).shouldBeFalse()
              it.test(arr3).shouldBeFalse()
              it.test(arr4).shouldBeFalse()
            }

            FilterExpression.compile(hasNested1).shouldBeRight().also {
              it.test(obj1).shouldBeFalse()
              it.test(obj2).shouldBeFalse()
              it.test(obj3).shouldBeFalse()
              it.test(obj4).shouldBeTrue()
              it.test(obj5).shouldBeTrue()
              it.test(obj6).shouldBeTrue()
              it.test(arr1).shouldBeFalse()
              it.test(arr2).shouldBeTrue()
              it.test(arr3).shouldBeTrue()
              it.test(arr4).shouldBeTrue()
            }
          }
        }
      }
    }

    context("The and operator") {
      context("when applied to two instances of And") {
        should("return a single And instance concatenating the operands of both") {
          val f1 =
            Or(
              nel(
                Existence(Relative()["a"]),
                Comparison(
                  Op.LESS,
                  Literal(1),
                  Literal(2),
                ),
              ),
            )

          val f2 = Existence(Relative()["b"])

          val f3 =
            Match(
              Relative()["c"],
              Literal("a.*"),
              true,
            )

          val f4 = Not(Existence(Absolute()["d"]))

          val f5 =
            Comparison(
              Op.GREATER,
              Absolute()["e"],
              Literal(2),
            )

          And(nel(f1, f2)) and And(nel(f3, f4)) shouldBe And(nel(f1, f2, f3, f4))
          And(nel(f1, f3)) and And(nel(f2, f5, f4)) shouldBe And(nel(f1, f3, f2, f5, f4))
          And(nel(f1, f3)) and And(nel(f2, f3, f5)) shouldBe And(nel(f1, f3, f2, f3, f5))
        }
      }

      context("when only the left hand operand is an instance of And") {
        should(
          "return a single And instance appending the right hand operand to the operands of the left hand And",
        ) {
          val f1 =
            Or(
              nel(
                Existence(Relative()["a"]),
                Comparison(
                  Op.LESS,
                  Literal(1),
                  Literal(2),
                ),
              ),
            )

          val f2 = Existence(Relative()["b"])

          val f3 =
            Match(
              Relative()["c"],
              Literal("a.*"),
              true,
            )

          val f4 = Not(Existence(Absolute()["d"]))

          val f5 =
            Comparison(
              Op.GREATER,
              Absolute()["e"],
              Literal(2),
            )

          And(nel(f1, f2)) and f3 shouldBe And(nel(f1, f2, f3))
          And(nel(f1, f3, f5)) and f4 shouldBe And(nel(f1, f3, f5, f4))
          And(nel(f1, f3, f5, f2)) and f2 and f4 shouldBe And(nel(f1, f3, f5, f2, f2, f4))
        }
      }

      context("when only the right hand operand is an instance of And") {
        should(
          "return a single And instance prepending the left hand operand to the operands of the right hand And",
        ) {
          val f1 =
            Or(
              nel(
                Existence(Relative()["a"]),
                Comparison(
                  Op.LESS,
                  Literal(1),
                  Literal(2),
                ),
              ),
            )

          val f2 = Existence(Relative()["b"])

          val f3 =
            Match(
              Relative()["c"],
              Literal("a.*"),
              true,
            )

          val f4 = Not(Existence(Absolute()["d"]))

          val f5 =
            Comparison(
              Op.GREATER,
              Absolute()["e"],
              Literal(2),
            )

          f3 and And(nel(f1, f2)) shouldBe And(nel(f3, f1, f2))
          f4 and And(nel(f1, f3, f5)) shouldBe And(nel(f4, f1, f3, f5))
          f2 and And(nel(f1, f3, f5, f2)) and f4 shouldBe And(nel(f2, f1, f3, f5, f2, f4))
        }
      }

      context("when none of the operands is an instance of And") {
        should("return an And instance containing both operands") {
          val f1 =
            Or(
              nel(
                Existence(Relative()["a"]),
                Comparison(
                  Op.LESS,
                  Literal(1),
                  Literal(2),
                ),
              ),
            )

          val f2 = Existence(Relative()["b"])

          val f3 =
            Match(
              Relative()["c"],
              Literal("a.*"),
              true,
            )

          f1 and f2 shouldBe And(nonEmptyListOf(f1, f2))
          f1 and f3 shouldBe And(nonEmptyListOf(f1, f3))
          f2 and f3 shouldBe And(nonEmptyListOf(f2, f3))
          f3 and f1 shouldBe And(nonEmptyListOf(f3, f1))
          f3 and f2 and f1 shouldBe And(nonEmptyListOf(f3, f2, f1))
        }
      }
    }

    context("The or operator") {
      context("when applied to two instances of Or") {
        should("return a single Or instance concatenating the operands of both") {
          val f1 =
            And(
              nel(
                Existence(Relative()["a"]),
                Comparison(
                  Op.LESS,
                  Literal(1),
                  Literal(2),
                ),
              ),
            )

          val f2 = Existence(Relative()["b"])

          val f3 =
            Match(
              Relative()["c"],
              Literal("a.*"),
              true,
            )

          val f4 = Not(Existence(Absolute()["d"]))

          val f5 =
            Comparison(
              Op.GREATER,
              Absolute()["e"],
              Literal(2),
            )

          Or(nel(f1, f2)) or Or(nel(f3, f4)) shouldBe Or(nel(f1, f2, f3, f4))
          Or(nel(f1, f3)) or Or(nel(f2, f5, f4)) shouldBe Or(nel(f1, f3, f2, f5, f4))
          Or(nel(f1, f3)) or Or(nel(f2, f3, f5)) shouldBe Or(nel(f1, f3, f2, f3, f5))
        }
      }

      context("when only the left hor operand is an instance of Or") {
        should(
          "return a single Or instance appending the right hor operand to the operands of the left hor Or",
        ) {
          val f1 =
            And(
              nel(
                Existence(Relative()["a"]),
                Comparison(
                  Op.LESS,
                  Literal(1),
                  Literal(2),
                ),
              ),
            )

          val f2 = Existence(Relative()["b"])

          val f3 =
            Match(
              Relative()["c"],
              Literal("a.*"),
              true,
            )

          val f4 = Not(Existence(Absolute()["d"]))

          val f5 =
            Comparison(
              Op.GREATER,
              Absolute()["e"],
              Literal(2),
            )

          Or(nel(f1, f2)) or f3 shouldBe Or(nel(f1, f2, f3))
          Or(nel(f1, f3, f5)) or f4 shouldBe Or(nel(f1, f3, f5, f4))
          Or(nel(f1, f3, f5, f2)) or f2 or f4 shouldBe Or(nel(f1, f3, f5, f2, f2, f4))
        }
      }

      context("when only the right hor operand is an instance of Or") {
        should(
          "return a single Or instance prepending the left hor operand to the operands of the right hor Or",
        ) {
          val f1 =
            And(
              nel(
                Existence(Relative()["a"]),
                Comparison(
                  Op.LESS,
                  Literal(1),
                  Literal(2),
                ),
              ),
            )

          val f2 = Existence(Relative()["b"])

          val f3 =
            Match(
              Relative()["c"],
              Literal("a.*"),
              true,
            )

          val f4 = Not(Existence(Absolute()["d"]))

          val f5 =
            Comparison(
              Op.GREATER,
              Absolute()["e"],
              Literal(2),
            )

          f3 or Or(nel(f1, f2)) shouldBe Or(nel(f3, f1, f2))
          f4 or Or(nel(f1, f3, f5)) shouldBe Or(nel(f4, f1, f3, f5))
          f2 or Or(nel(f1, f3, f5, f2)) or f4 shouldBe Or(nel(f2, f1, f3, f5, f2, f4))
        }
      }

      context("when none of the operands is an instance of Or") {
        should("return an Or instance containing both operands") {
          val f1 =
            And(
              nel(
                Existence(Relative()["a"]),
                Comparison(
                  Op.LESS,
                  Literal(1),
                  Literal(2),
                ),
              ),
            )

          val f2 = Existence(Relative()["b"])

          val f3 =
            Match(
              Relative()["c"],
              Literal("a.*"),
              true,
            )

          f1 or f2 shouldBe Or(nonEmptyListOf(f1, f2))
          f1 or f3 shouldBe Or(nonEmptyListOf(f1, f3))
          f2 or f3 shouldBe Or(nonEmptyListOf(f2, f3))
          f3 or f1 shouldBe Or(nonEmptyListOf(f3, f1))
          f3 or f2 or f1 shouldBe Or(nonEmptyListOf(f3, f2, f1))
        }
      }
    }

    context("The not operator") {
      context("when applied to an instance of Not") {
        should("return the operand of Not") {
          val f1 =
            And(
              nel(
                Existence(Relative()["a"]),
                Comparison(
                  Op.LESS,
                  Literal(1),
                  Literal(2),
                ),
              ),
            )

          val f2 = Existence(Relative()["b"])

          val f3 =
            Match(
              Relative()["c"],
              Literal("a.*"),
              true,
            )

          !Not(f1) shouldBe f1
          !Not(f2) shouldBe f2
          !Not(f3) shouldBe f3
        }
      }

      context("when applied to an instance of Comparison") {
        should("invert the comparison operator") {
          checkAll(Arb.comparable(), Arb.comparable()) { lhs, rhs ->
            !Comparison(Op.EQ, lhs, rhs) shouldBe Comparison(Op.NOT_EQ, lhs, rhs)
            !Comparison(Op.NOT_EQ, lhs, rhs) shouldBe Comparison(Op.EQ, lhs, rhs)
            !Comparison(Op.LESS, lhs, rhs) shouldBe Comparison(Op.GREATER_EQ, lhs, rhs)
            !Comparison(Op.LESS_EQ, lhs, rhs) shouldBe Comparison(Op.GREATER, lhs, rhs)
            !Comparison(Op.GREATER, lhs, rhs) shouldBe Comparison(Op.LESS_EQ, lhs, rhs)
            !Comparison(Op.GREATER_EQ, lhs, rhs) shouldBe Comparison(Op.LESS, lhs, rhs)
          }
        }
      }

      context("when the operand is not an instance of Not") {
        should("return a Not instance of the operand") {
          val f1 =
            And(
              nel(
                Existence(Relative()["a"]),
                Comparison(
                  Op.LESS,
                  Literal(1),
                  Literal(2),
                ),
              ),
            )

          val f2 = Existence(Relative()["b"])

          val f3 =
            Match(
              Relative()["c"],
              Literal("a.*"),
              true,
            )

          !f1 shouldBe Not(f1)
          !f2 shouldBe Not(f2)
          !f3 shouldBe Not(f3)
        }
      }
    }

    context("The toString method") {
      context("of an And expression") {
        should("concatenate the serialized operands with &&, parenthesizing Or instances") {
          val f1 =
            Or(
              Existence(Relative()["a"]),
              Comparison(
                Op.LESS,
                Literal(1),
                Literal(2),
              ),
            )

          val f2 = Existence(Relative()["b"])

          val f3 =
            Match(
              Relative()["c"],
              Literal("a.*"),
              true,
            )

          val f4 = Not(Existence(Absolute()["d"]))

          val f5 =
            Comparison(
              Op.GREATER,
              Absolute()["e"],
              Literal(2),
            )

          And(f1, f2, f3, f4).toString() shouldBe "($f1) && $f2 && $f3 && $f4"
          And(f5, f1, f2).toString() shouldBe "$f5 && ($f1) && $f2"
          And(f3, f1).toString() shouldBe "$f3 && ($f1)"
        }
      }

      context("of an Or expression") {
        should("concatenate the serialized operands with ||") {
          val f1 =
            And(
              Existence(Relative()["a"]),
              Comparison(
                Op.LESS,
                Literal(1),
                Literal(2),
              ),
            )

          val f2 = Existence(Relative()["b"])

          val f3 =
            Match(
              Relative()["c"],
              Literal("a.*"),
              true,
            )

          val f4 = Not(Existence(Absolute()["d"]))

          val f5 =
            Comparison(
              Op.GREATER,
              Absolute()["e"],
              Literal(2),
            )

          Or(f1, f2, f3, f4).toString() shouldBe "$f1 || $f2 || $f3 || $f4"
          Or(f5, f1, f2).toString() shouldBe "$f5 || $f1 || $f2"
          Or(f3, f1).toString() shouldBe "$f3 || $f1"
        }
      }

      context("of a Not expression") {
        should(
          "prepend an exclamation mark to the serialized operand, parenthesizing Or, Comparision and And",
        ) {
          val f1 =
            And(
              Existence(Relative()["a"]),
              Comparison(
                Op.LESS,
                Literal(1),
                Literal(2),
              ),
            )

          val f2 =
            Or(
              Existence(Relative()["a"]),
              Comparison(
                Op.LESS,
                Literal(1),
                Literal(2),
              ),
            )

          val f3 = Existence(Relative()["b"])

          val f4 =
            Match(
              Relative()["c"],
              Literal("a.*"),
              true,
            )

          val f5 =
            Comparison(
              Op.GREATER,
              Absolute()["e"],
              Literal(2),
            )

          Not(f1).toString() shouldBe "!($f1)"
          Not(f2).toString() shouldBe "!($f2)"
          Not(f3).toString() shouldBe "!$f3"
          Not(f4).toString() shouldBe "!$f4"
          Not(f5).toString() shouldBe "!($f5)"
        }
      }

      context("of an Existence expression") {
        should("return the serialized query") {
          checkAll(Arb.queryExpression()) {
            Existence(it).toString() shouldBe it.toString()
          }
        }
      }

      context("of a Match expression") {
        should("serialize to a match function expression if matchEntire = true") {
          checkAll(Arb.matchOperand(), Arb.matchOperand()) { subject, pattern ->
            Match(subject, pattern, matchEntire = true).toString() shouldBe
              "match($subject, $pattern)"
          }
        }

        should("serialize to a search function expression if matchEntire = false") {
          checkAll(Arb.matchOperand(), Arb.matchOperand()) { subject, pattern ->
            Match(subject, pattern, matchEntire = false).toString() shouldBe
              "search($subject, $pattern)"
          }
        }
      }
    }
  })
