package com.kobil.vertx.jsonpath

import com.kobil.vertx.jsonpath.JsonPath.Companion.onlyPaths
import com.kobil.vertx.jsonpath.testing.normalizedJsonPath
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeNone
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.arrow.core.shouldBeSome
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.property.Arb
import io.kotest.property.arbitrary.list
import io.kotest.property.checkAll
import io.vertx.kotlin.core.json.jsonArrayOf
import io.vertx.kotlin.core.json.jsonObjectOf

class JsonPathTest :
  ShouldSpec({
    context("The evaluateSingle function") {
      context("called on a JSON object") {
        should("return the single result if there is exactly one") {
          val jsonPath1 = JsonPath.compile("$.a").shouldBeRight()
          val jsonPath2 = JsonPath.compile("$.a[1]['b']").shouldBeRight()

          jsonPath1
            .evaluateOne(jsonObjectOf("a" to 1, "b" to 2))
            .shouldBeRight() shouldBeSome JsonNode(1, JsonPath["a"])
          jsonObjectOf("a" to 1, "b" to 2)
            .get<Int>(jsonPath1)
            .shouldBeRight() shouldBeSome 1

          jsonPath1
            .evaluateOne(jsonObjectOf("a" to "string"))
            .shouldBeRight() shouldBeSome JsonNode("string", JsonPath["a"])
          jsonObjectOf("a" to "string")
            .get<String>(jsonPath1)
            .shouldBeRight() shouldBeSome "string"

          jsonPath2
            .evaluateOne(jsonObjectOf("a" to jsonArrayOf(null, jsonObjectOf("b" to true))))
            .shouldBeRight() shouldBeSome JsonNode(true, JsonPath["a"][1]["b"])
          jsonObjectOf("a" to jsonArrayOf(null, jsonObjectOf("b" to true)))
            .get<Boolean>(jsonPath2)
            .shouldBeRight() shouldBeSome true
        }

        should("return None if there is no result") {
          val jsonPath1 = JsonPath.compile("$.a").shouldBeRight()
          val jsonPath2 = JsonPath.compile("$.a[1]['b']").shouldBeRight()

          jsonPath1.evaluateOne(jsonObjectOf("b" to 2)).shouldBeRight().shouldBeNone()

          jsonPath1
            .evaluateOne(jsonObjectOf("b" to jsonObjectOf("a" to 1)))
            .shouldBeRight()
            .shouldBeNone()

          jsonPath2
            .evaluateOne(jsonObjectOf("b" to jsonArrayOf(null, jsonObjectOf("b" to true))))
            .shouldBeRight()
            .shouldBeNone()

          jsonPath2
            .evaluateOne(jsonObjectOf("a" to jsonArrayOf(jsonObjectOf("b" to true))))
            .shouldBeRight()
            .shouldBeNone()

          jsonPath2
            .evaluateOne(jsonObjectOf("a" to jsonArrayOf(null, jsonObjectOf("a" to true))))
            .shouldBeRight()
            .shouldBeNone()
        }

        should("return an error if there are multiple results") {
          val jsonPath1 = JsonPath.compile("$..a").shouldBeRight()
          val jsonPath2 = JsonPath.compile("$[?@.a]").shouldBeRight()

          jsonPath1
            .evaluateOne(
              jsonObjectOf(
                "a" to 1,
                "b" to jsonObjectOf("a" to 2),
                "c" to jsonArrayOf(1, jsonObjectOf("a" to 3)),
              ),
            ).shouldBeLeft()
            .results
            .shouldContainExactlyInAnyOrder(
              JsonNode(1, JsonPath["a"]),
              JsonNode(2, JsonPath["b"]["a"]),
              JsonNode(3, JsonPath["c"][1]["a"]),
            )

          jsonPath1
            .evaluateOne(jsonObjectOf("a" to "string", "b" to jsonObjectOf("a" to null)))
            .shouldBeLeft()
            .results
            .shouldContainExactlyInAnyOrder(
              JsonNode("string", JsonPath["a"]),
              JsonNode(null, JsonPath["b"]["a"]),
            )

          jsonPath2
            .evaluateOne(
              jsonObjectOf(
                "a" to jsonObjectOf("a" to 1),
                "b" to jsonObjectOf("a" to 2),
              ),
            ).shouldBeLeft()
            .results
            .shouldContainExactlyInAnyOrder(
              JsonNode(jsonObjectOf("a" to 1), JsonPath["a"]),
              JsonNode(jsonObjectOf("a" to 2), JsonPath["b"]),
            )
        }
      }

      context("called on a JSON array") {
        should("return the single result if there is exactly one") {
          val jsonPath1 = JsonPath.compile("$[0]").shouldBeRight()
          val jsonPath2 = JsonPath.compile("$[1]['b']").shouldBeRight()

          jsonPath1
            .evaluateOne(jsonArrayOf(1, 2, 3))
            .shouldBeRight() shouldBeSome JsonNode(1, JsonPath[0])

          jsonPath1
            .evaluateOne(jsonArrayOf("string", null, true))
            .shouldBeRight() shouldBeSome JsonNode("string", JsonPath[0])

          jsonPath2
            .evaluateOne(jsonArrayOf(null, jsonObjectOf("b" to true)))
            .shouldBeRight() shouldBeSome JsonNode(true, JsonPath[1]["b"])
        }

        should("return None if there is no result") {
          val jsonPath1 = JsonPath.compile("$[0]").shouldBeRight()
          val jsonPath2 = JsonPath.compile("$[1]['b']").shouldBeRight()

          jsonPath1.evaluateOne(jsonArrayOf()).shouldBeRight().shouldBeNone()

          jsonPath2
            .evaluateOne(jsonArrayOf(null, jsonObjectOf("a" to true)))
            .shouldBeRight()
            .shouldBeNone()

          jsonPath2
            .evaluateOne(jsonArrayOf(jsonObjectOf("b" to true)))
            .shouldBeRight()
            .shouldBeNone()
        }

        should("return an error if there are multiple results") {
          val jsonPath1 = JsonPath.compile("$..a").shouldBeRight()
          val jsonPath2 = JsonPath.compile("$[?@.a]").shouldBeRight()

          jsonPath1
            .evaluateOne(
              jsonArrayOf(
                jsonObjectOf("a" to 2),
                jsonArrayOf(1, jsonObjectOf("a" to 3)),
              ),
            ).shouldBeLeft()
            .results
            .shouldContainExactlyInAnyOrder(
              JsonNode(2, JsonPath[0]["a"]),
              JsonNode(3, JsonPath[1][1]["a"]),
            )

          jsonPath2
            .evaluateOne(
              jsonArrayOf(
                jsonObjectOf("a" to 1),
                true,
                jsonObjectOf("b" to 3),
                jsonObjectOf("a" to 2),
              ),
            ).shouldBeLeft()
            .results
            .shouldContainExactlyInAnyOrder(
              JsonNode(jsonObjectOf("a" to 1), JsonPath[0]),
              JsonNode(jsonObjectOf("a" to 2), JsonPath[3]),
            )
        }
      }
    }

    context("The onlyPaths function") {
      should("return exactly the path fields of the list of JsonNodes") {
        checkAll(Arb.list(Arb.normalizedJsonPath(), 0..32)) { paths ->
          paths.map { JsonNode(null, it) }.onlyPaths() shouldContainExactly paths
        }
      }
    }

    context("The selector constructor") {
      context("taking an IntProgression") {
        should("return a proper slice selector") {
          val jp1 = JsonPath[Selector(1..6 step 2)]

          jp1.evaluate(jsonArrayOf(0, 1, 2, 3, 4, 5, 6)).shouldContainExactly(
            JsonNode(1, JsonPath[1]),
            JsonNode(3, JsonPath[3]),
            JsonNode(5, JsonPath[5]),
          )

          val jp2 = JsonPath[Selector(6 downTo 0 step 2)]

          jp2.evaluate(jsonArrayOf(0, 1, 2, 3, 4, 5, 6)).shouldContainExactly(
            JsonNode(6, JsonPath[6]),
            JsonNode(4, JsonPath[4]),
            JsonNode(2, JsonPath[2]),
            JsonNode(0, JsonPath[0]),
          )

          val jp3 = JsonPath[Selector(-1 downTo -3 step 1)]

          jp3.evaluate(jsonArrayOf(0, 1, 2, 3, 4, 5, 6)).shouldContainExactly(
            JsonNode(6, JsonPath[6]),
            JsonNode(5, JsonPath[5]),
            JsonNode(4, JsonPath[4]),
          )
        }
      }

      context("taking three nullable integers") {
        should("return a proper slice selector") {
          val jp1 = JsonPath[Selector(1, 6, 2)]

          jp1.evaluate(jsonArrayOf(0, 1, 2, 3, 4, 5, 6)).shouldContainExactly(
            JsonNode(1, JsonPath[1]),
            JsonNode(3, JsonPath[3]),
            JsonNode(5, JsonPath[5]),
          )

          val jp2 = JsonPath[Selector(6, null, -2)]

          jp2.evaluate(jsonArrayOf(0, 1, 2, 3, 4, 5, 6)).shouldContainExactly(
            JsonNode(6, JsonPath[6]),
            JsonNode(4, JsonPath[4]),
            JsonNode(2, JsonPath[2]),
            JsonNode(0, JsonPath[0]),
          )

          val jp3 = JsonPath[Selector(-1, -4, -1)]

          jp3.evaluate(jsonArrayOf(0, 1, 2, 3, 4, 5, 6)).shouldContainExactly(
            JsonNode(6, JsonPath[6]),
            JsonNode(5, JsonPath[5]),
            JsonNode(4, JsonPath[4]),
          )

          val jp4 = JsonPath[Selector(2, 5)]

          jp4.evaluate(jsonArrayOf(0, 1, 2, 3, 4, 5, 6)).shouldContainExactly(
            JsonNode(2, JsonPath[2]),
            JsonNode(3, JsonPath[3]),
            JsonNode(4, JsonPath[4]),
          )
        }
      }

      context("taking a filter") {
        should("return a proper filter Selector") {
          val jp1 =
            JsonPath[
              Selector(
                FilterExpression.Comparison(
                  FilterExpression.Comparison.Op.LESS,
                  QueryExpression.Relative(),
                  ComparableExpression.Literal(3),
                ),
              ),
            ]

          jp1.evaluate(jsonArrayOf(0, 1, 2, 3, 4, 5, 6)).shouldContainExactly(
            JsonNode(0, JsonPath[0]),
            JsonNode(1, JsonPath[1]),
            JsonNode(2, JsonPath[2]),
          )

          val jp2 =
            JsonPath[
              Selector(
                FilterExpression.Match(
                  QueryExpression.Relative(listOf(Segment.ChildSegment("a"))),
                  ComparableExpression.Literal("a.*"),
                  matchEntire = true,
                ),
              ),
            ]

          jp2
            .evaluate(
              jsonArrayOf(
                jsonObjectOf("a" to "a"),
                jsonObjectOf("a" to "bc"),
                jsonObjectOf("a" to "abc"),
                jsonObjectOf("b" to "ab"),
              ),
            ).shouldContainExactly(
              JsonNode(jsonObjectOf("a" to "a"), JsonPath[0]),
              JsonNode(jsonObjectOf("a" to "abc"), JsonPath[2]),
            )
        }
      }
    }
  })
