package com.kobil.vertx.jsonpath

import com.kobil.vertx.jsonpath.FilterExpression.Comparison
import com.kobil.vertx.jsonpath.testing.comparable
import com.kobil.vertx.jsonpath.testing.matchOperand
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.choice
import io.kotest.property.arbitrary.constant
import io.kotest.property.arbitrary.double
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

class ComparableExpressionTest :
  ShouldSpec({
    context("The eq operator") {
      should("return a Comparison of both operands using the EQ operator") {
        checkAll(Arb.comparable(), Arb.comparable()) { lhs, rhs ->
          lhs eq rhs shouldBe Comparison(Comparison.Op.EQ, lhs, rhs)
        }
      }
    }

    context("The neq operator") {
      should("return a Comparison of both operands using the NOT_EQ operator") {
        checkAll(Arb.comparable(), Arb.comparable()) { lhs, rhs ->
          lhs neq rhs shouldBe Comparison(Comparison.Op.NOT_EQ, lhs, rhs)
        }
      }
    }

    context("The gt operator") {
      should("return a Comparison of both operands using the GREATER operator") {
        checkAll(Arb.comparable(), Arb.comparable()) { lhs, rhs ->
          lhs gt rhs shouldBe Comparison(Comparison.Op.GREATER, lhs, rhs)
        }
      }
    }

    context("The ge operator") {
      should("return a Comparison of both operands using the GREATER_EQ operator") {
        checkAll(Arb.comparable(), Arb.comparable()) { lhs, rhs ->
          lhs ge rhs shouldBe Comparison(Comparison.Op.GREATER_EQ, lhs, rhs)
        }
      }
    }

    context("The lt operator") {
      should("return a Comparison of both operands using the LESS operator") {
        checkAll(Arb.comparable(), Arb.comparable()) { lhs, rhs ->
          lhs lt rhs shouldBe Comparison(Comparison.Op.LESS, lhs, rhs)
        }
      }
    }

    context("The le operator") {
      should("return a Comparison of both operands using the LESS_EQ operator") {
        checkAll(Arb.comparable(), Arb.comparable()) { lhs, rhs ->
          lhs le rhs shouldBe Comparison(Comparison.Op.LESS_EQ, lhs, rhs)
        }
      }
    }

    context("The length function") {
      should("return a Length instance with the given operand") {
        checkAll(Arb.comparable()) { lhs ->
          lhs.length() shouldBe FunctionExpression.Length(lhs)
        }
      }
    }

    context("The match function") {
      should("return a Match instance with the given subject and pattern and matchEntire = true") {
        checkAll(Arb.matchOperand(), Arb.matchOperand()) { subject, pattern ->
          subject.match(pattern) shouldBe
            FilterExpression.Match(subject, pattern, matchEntire = true)
        }
      }
    }

    context("The search function") {
      should("return a Match instance with the given subject and pattern and matchEntire = false") {
        checkAll(Arb.matchOperand(), Arb.matchOperand()) { subject, pattern ->
          subject.search(pattern) shouldBe
            FilterExpression.Match(subject, pattern, matchEntire = false)
        }
      }
    }

    context("The toString method") {
      context("of a Literal instance") {
        should("wrap a string value in double quotes") {
          checkAll(Arb.string()) {
            ComparableExpression.Literal(it).toString() shouldBe "\"$it\""
          }
        }

        should("simply serialize any other value to string") {
          checkAll(
            Arb.choice(Arb.int(), Arb.double(), Arb.boolean(), Arb.constant(null)),
          ) {
            ComparableExpression.Literal(it).toString() shouldBe "$it"
          }
        }
      }
    }
  })
