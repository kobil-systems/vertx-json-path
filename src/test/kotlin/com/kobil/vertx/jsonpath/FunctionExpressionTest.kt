package com.kobil.vertx.jsonpath

import com.kobil.vertx.jsonpath.testing.comparable
import com.kobil.vertx.jsonpath.testing.queryExpression
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.checkAll

class FunctionExpressionTest :
  ShouldSpec({
    context("the toString method") {
      context("of the Length function") {
        should("serialize to 'length' followed by the parenthesized serialized argument") {
          checkAll(Arb.comparable()) {
            FunctionExpression.Length(it).toString() shouldBe "length($it)"
          }
        }
      }

      context("of the Count function") {
        should("serialize to 'count' followed by the parenthesized serialized argument") {
          checkAll(Arb.queryExpression()) {
            FunctionExpression.Count(it).toString() shouldBe "count($it)"
          }
        }
      }

      context("of the Value function") {
        should("serialize to 'value' followed by the parenthesized serialized argument") {
          checkAll(Arb.queryExpression()) {
            FunctionExpression.Value(it).toString() shouldBe "value($it)"
          }
        }
      }
    }
  })
