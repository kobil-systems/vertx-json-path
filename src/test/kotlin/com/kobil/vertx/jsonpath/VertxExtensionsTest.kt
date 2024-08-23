package com.kobil.vertx.jsonpath

import com.kobil.vertx.jsonpath.error.MultipleResults
import com.kobil.vertx.jsonpath.error.RequiredJsonValueError
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeNone
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.arrow.core.shouldBeSome
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.jsonArrayOf
import io.vertx.kotlin.core.json.jsonObjectOf

class VertxExtensionsTest :
  ShouldSpec({
    context("The get operator") {
      context("applied to a JsonObject") {
        should("return the single result if there is exactly one") {
          val jsonPath1 = JsonPath.compile("$.a").shouldBeRight()
          val jsonPath2 = JsonPath.compile("$.a[1]['b']").shouldBeRight()

          jsonObjectOf("a" to 1, "b" to 2)
            .get<Int>(jsonPath1)
            .shouldBeRight() shouldBeSome 1

          jsonObjectOf("a" to "string")
            .get<String>(jsonPath1)
            .shouldBeRight() shouldBeSome "string"

          jsonObjectOf("a" to jsonArrayOf(null, jsonObjectOf("b" to true)))
            .get<Boolean>(jsonPath2)
            .shouldBeRight() shouldBeSome true
        }

        should("return None if there is no result") {
          val jsonPath1 = JsonPath.compile("$.a").shouldBeRight()
          val jsonPath2 = JsonPath.compile("$.a[1]['b']").shouldBeRight()

          jsonPath1.evaluateOne(jsonObjectOf("b" to 2)).shouldBeRight().shouldBeNone()

          jsonObjectOf("b" to jsonObjectOf("a" to 1))
            .get<Int>(jsonPath1)
            .shouldBeRight()
            .shouldBeNone()

          jsonObjectOf("b" to jsonArrayOf(null, jsonObjectOf("b" to true)))
            .get<Any?>(jsonPath2)
            .shouldBeRight()
            .shouldBeNone()

          jsonObjectOf("a" to jsonArrayOf(jsonObjectOf("b" to true)))
            .get<Any?>(jsonPath2)
            .shouldBeRight()
            .shouldBeNone()

          jsonObjectOf("a" to jsonArrayOf(null, jsonObjectOf("a" to true)))
            .get<Any?>(jsonPath2)
            .shouldBeRight()
            .shouldBeNone()
        }

        should("return an error if there are multiple results") {
          val jsonPath1 = JsonPath.compile("$..a").shouldBeRight()
          val jsonPath2 = JsonPath.compile("$[?@.a]").shouldBeRight()

          jsonObjectOf(
            "a" to 1,
            "b" to jsonObjectOf("a" to 2),
            "c" to jsonArrayOf(1, jsonObjectOf("a" to 3)),
          ).get<Int>(jsonPath1)
            .shouldBeLeft()
            .results
            .shouldContainExactlyInAnyOrder(
              JsonNode(1, JsonPath["a"]),
              JsonNode(2, JsonPath["b"]["a"]),
              JsonNode(3, JsonPath["c"][1]["a"]),
            )

          jsonObjectOf("a" to "string", "b" to jsonObjectOf("a" to null))
            .get<String?>(jsonPath1)
            .shouldBeLeft()
            .results
            .shouldContainExactlyInAnyOrder(
              JsonNode("string", JsonPath["a"]),
              JsonNode(null, JsonPath["b"]["a"]),
            )

          jsonObjectOf("a" to jsonObjectOf("a" to 1), "b" to jsonObjectOf("a" to 2))
            .get<JsonObject>(jsonPath2)
            .shouldBeLeft()
            .results
            .shouldContainExactlyInAnyOrder(
              JsonNode(jsonObjectOf("a" to 1), JsonPath["a"]),
              JsonNode(jsonObjectOf("a" to 2), JsonPath["b"]),
            )
        }
      }

      context("applied to a JsonArray") {
        should("return the single result if there is exactly one") {
          val jsonPath1 = JsonPath.compile("$[0]").shouldBeRight()
          val jsonPath2 = JsonPath.compile("$[1]['b']").shouldBeRight()

          jsonArrayOf(1, 2, 3)
            .get<Int>(jsonPath1)
            .shouldBeRight() shouldBeSome 1

          jsonArrayOf("string", null, true)
            .get<String>(jsonPath1)
            .shouldBeRight() shouldBeSome "string"

          jsonArrayOf(null, jsonObjectOf("b" to true))
            .get<Boolean>(jsonPath2)
            .shouldBeRight() shouldBeSome true
        }

        should("return None if there is no result") {
          val jsonPath1 = JsonPath.compile("$[0]").shouldBeRight()
          val jsonPath2 = JsonPath.compile("$[1]['b']").shouldBeRight()

          jsonArrayOf()
            .get<Any?>(jsonPath1)
            .shouldBeRight()
            .shouldBeNone()

          jsonArrayOf(null, jsonObjectOf("a" to true))
            .get<Any?>(jsonPath2)
            .shouldBeRight()
            .shouldBeNone()

          jsonArrayOf(jsonObjectOf("b" to true))
            .get<Any?>(jsonPath2)
            .shouldBeRight()
            .shouldBeNone()
        }

        should("return an error if there are multiple results") {
          val jsonPath1 = JsonPath.compile("$..a").shouldBeRight()
          val jsonPath2 = JsonPath.compile("$[?@.a]").shouldBeRight()

          jsonArrayOf(jsonObjectOf("a" to 2), jsonArrayOf(1, jsonObjectOf("a" to 3)))
            .get<Int>(jsonPath1)
            .shouldBeLeft()
            .results
            .shouldContainExactly(
              JsonNode(2, JsonPath[0]["a"]),
              JsonNode(3, JsonPath[1][1]["a"]),
            )

          jsonArrayOf(
            jsonObjectOf("a" to 1),
            true,
            jsonObjectOf("b" to 3),
            jsonObjectOf("a" to 2),
          ).get<JsonObject>(jsonPath2)
            .shouldBeLeft()
            .results
            .shouldContainExactly(
              JsonNode(jsonObjectOf("a" to 1), JsonPath[0]),
              JsonNode(jsonObjectOf("a" to 2), JsonPath[3]),
            )
        }
      }
    }

    context("The required function") {
      context("applied to a JsonObject") {
        should("return the single result if there is exactly one") {
          val jsonPath1 = JsonPath.compile("$.a").shouldBeRight()
          val jsonPath2 = JsonPath.compile("$.a[1]['b']").shouldBeRight()

          jsonObjectOf("a" to 1, "b" to 2)
            .required<Int>(jsonPath1)
            .shouldBeRight(1)

          jsonObjectOf("a" to "string")
            .required<String>(jsonPath1)
            .shouldBeRight("string")

          jsonObjectOf("a" to jsonArrayOf(null, jsonObjectOf("b" to true)))
            .required<Boolean>(jsonPath2)
            .shouldBeRight(true)
        }

        should("return None if there is no result") {
          val jsonPath1 = JsonPath.compile("$.a").shouldBeRight()
          val jsonPath2 = JsonPath.compile("$.a[1]['b']").shouldBeRight()

          jsonPath1.evaluateOne(jsonObjectOf("b" to 2)).shouldBeRight().shouldBeNone()

          jsonObjectOf("b" to jsonObjectOf("a" to 1))
            .required<Int>(jsonPath1)
            .shouldBeLeft(RequiredJsonValueError.NoResult)

          jsonObjectOf("b" to jsonArrayOf(null, jsonObjectOf("b" to true)))
            .required<Any?>(jsonPath2)
            .shouldBeLeft(RequiredJsonValueError.NoResult)

          jsonObjectOf("a" to jsonArrayOf(jsonObjectOf("b" to true)))
            .required<Any?>(jsonPath2)
            .shouldBeLeft(RequiredJsonValueError.NoResult)

          jsonObjectOf("a" to jsonArrayOf(null, jsonObjectOf("a" to true)))
            .required<Any?>(jsonPath2)
            .shouldBeLeft(RequiredJsonValueError.NoResult)
        }

        should("return an error if there are multiple results") {
          val jsonPath1 = JsonPath.compile("$..a").shouldBeRight()
          val jsonPath2 = JsonPath.compile("$[?@.a]").shouldBeRight()

          jsonObjectOf(
            "a" to 1,
            "b" to jsonObjectOf("a" to 2),
            "c" to jsonArrayOf(1, jsonObjectOf("a" to 3)),
          ).required<Int>(jsonPath1)
            .shouldBeLeft()
            .shouldBeInstanceOf<MultipleResults>()
            .results
            .shouldContainExactlyInAnyOrder(
              JsonNode(1, JsonPath["a"]),
              JsonNode(2, JsonPath["b"]["a"]),
              JsonNode(3, JsonPath["c"][1]["a"]),
            )

          jsonObjectOf("a" to "string", "b" to jsonObjectOf("a" to null))
            .required<String?>(jsonPath1)
            .shouldBeLeft()
            .shouldBeInstanceOf<MultipleResults>()
            .results
            .shouldContainExactlyInAnyOrder(
              JsonNode("string", JsonPath["a"]),
              JsonNode(null, JsonPath["b"]["a"]),
            )

          jsonObjectOf("a" to jsonObjectOf("a" to 1), "b" to jsonObjectOf("a" to 2))
            .required<JsonObject>(jsonPath2)
            .shouldBeLeft()
            .shouldBeInstanceOf<MultipleResults>()
            .results
            .shouldContainExactlyInAnyOrder(
              JsonNode(jsonObjectOf("a" to 1), JsonPath["a"]),
              JsonNode(jsonObjectOf("a" to 2), JsonPath["b"]),
            )
        }
      }

      context("applied to a JsonArray") {
        should("return the single result if there is exactly one") {
          val jsonPath1 = JsonPath.compile("$[0]").shouldBeRight()
          val jsonPath2 = JsonPath.compile("$[1]['b']").shouldBeRight()

          jsonArrayOf(1, 2, 3)
            .required<Int>(jsonPath1)
            .shouldBeRight(1)

          jsonArrayOf("string", null, true)
            .required<String>(jsonPath1)
            .shouldBeRight("string")

          jsonArrayOf(null, jsonObjectOf("b" to true))
            .required<Boolean>(jsonPath2)
            .shouldBeRight(true)
        }

        should("return None if there is no result") {
          val jsonPath1 = JsonPath.compile("$[0]").shouldBeRight()
          val jsonPath2 = JsonPath.compile("$[1]['b']").shouldBeRight()

          jsonArrayOf()
            .required<Any?>(jsonPath1)
            .shouldBeLeft(RequiredJsonValueError.NoResult)

          jsonArrayOf(null, jsonObjectOf("a" to true))
            .required<Any?>(jsonPath2)
            .shouldBeLeft(RequiredJsonValueError.NoResult)

          jsonArrayOf(jsonObjectOf("b" to true))
            .required<Any?>(jsonPath2)
            .shouldBeLeft(RequiredJsonValueError.NoResult)
        }

        should("return an error if there are multiple results") {
          val jsonPath1 = JsonPath.compile("$..a").shouldBeRight()
          val jsonPath2 = JsonPath.compile("$[?@.a]").shouldBeRight()

          jsonArrayOf(jsonObjectOf("a" to 2), jsonArrayOf(1, jsonObjectOf("a" to 3)))
            .required<Int>(jsonPath1)
            .shouldBeLeft()
            .shouldBeInstanceOf<MultipleResults>()
            .results
            .shouldContainExactly(
              JsonNode(2, JsonPath[0]["a"]),
              JsonNode(3, JsonPath[1][1]["a"]),
            )

          jsonArrayOf(
            jsonObjectOf("a" to 1),
            true,
            jsonObjectOf("b" to 3),
            jsonObjectOf("a" to 2),
          ).required<JsonObject>(jsonPath2)
            .shouldBeLeft()
            .shouldBeInstanceOf<MultipleResults>()
            .results
            .shouldContainExactly(
              JsonNode(jsonObjectOf("a" to 1), JsonPath[0]),
              JsonNode(jsonObjectOf("a" to 2), JsonPath[3]),
            )
        }
      }
    }

    context("The getAll function") {
      context("applied to a JsonObject") {
        should("return a list containing the single result if there is exactly one") {
          val jsonPath1 = JsonPath.compile("$.a").shouldBeRight()
          val jsonPath2 = JsonPath.compile("$.a[1]['b']").shouldBeRight()

          jsonObjectOf("a" to 1, "b" to 2)
            .getAll<Int>(jsonPath1) shouldBe listOf(1)

          jsonObjectOf("a" to "string")
            .getAll<String>(jsonPath1) shouldBe listOf("string")

          jsonObjectOf("a" to jsonArrayOf(null, jsonObjectOf("b" to true)))
            .getAll<Boolean>(jsonPath2) shouldBe listOf(true)
        }

        should("return an empty list if there is no result") {
          val jsonPath1 = JsonPath.compile("$.a").shouldBeRight()
          val jsonPath2 = JsonPath.compile("$.a[1]['b']").shouldBeRight()

          jsonPath1.evaluateOne(jsonObjectOf("b" to 2)).shouldBeRight().shouldBeNone()

          jsonObjectOf("b" to jsonObjectOf("a" to 1))
            .getAll<Int>(jsonPath1) shouldBe listOf()

          jsonObjectOf("b" to jsonArrayOf(null, jsonObjectOf("b" to true)))
            .getAll<Any?>(jsonPath2) shouldBe listOf()

          jsonObjectOf("a" to jsonArrayOf(jsonObjectOf("b" to true)))
            .getAll<Any?>(jsonPath2) shouldBe listOf()

          jsonObjectOf("a" to jsonArrayOf(null, jsonObjectOf("a" to true)))
            .getAll<Any?>(jsonPath2) shouldBe listOf()
        }

        should("return all results if there are multiple") {
          val jsonPath1 = JsonPath.compile("$..a").shouldBeRight()
          val jsonPath2 = JsonPath.compile("$[?@.a]").shouldBeRight()

          jsonObjectOf(
            "a" to 1,
            "b" to jsonObjectOf("a" to 2),
            "c" to jsonArrayOf(1, jsonObjectOf("a" to 3)),
          ).getAll<Int>(jsonPath1) shouldContainExactlyInAnyOrder listOf(1, 2, 3)

          jsonObjectOf("a" to "string", "b" to jsonObjectOf("a" to null))
            .getAll<String?>(jsonPath1) shouldContainExactlyInAnyOrder listOf("string", null)

          jsonObjectOf("a" to jsonObjectOf("a" to 1), "b" to jsonObjectOf("a" to 2))
            .getAll<JsonObject>(jsonPath2) shouldContainExactlyInAnyOrder
            listOf(
              jsonObjectOf("a" to 1),
              jsonObjectOf("a" to 2),
            )
        }
      }

      context("applied to a JsonArray") {
        should("return the single result if there is exactly one") {
          val jsonPath1 = JsonPath.compile("$[0]").shouldBeRight()
          val jsonPath2 = JsonPath.compile("$[1]['b']").shouldBeRight()

          jsonArrayOf(1, 2, 3)
            .getAll<Int>(jsonPath1) shouldBe listOf(1)

          jsonArrayOf("string", null, true)
            .getAll<String>(jsonPath1) shouldBe listOf("string")

          jsonArrayOf(null, jsonObjectOf("b" to true))
            .getAll<Boolean>(jsonPath2) shouldBe listOf(true)
        }

        should("return None if there is no result") {
          val jsonPath1 = JsonPath.compile("$[0]").shouldBeRight()
          val jsonPath2 = JsonPath.compile("$[1]['b']").shouldBeRight()

          jsonArrayOf()
            .getAll<Any?>(jsonPath1) shouldBe listOf()

          jsonArrayOf(null, jsonObjectOf("a" to true))
            .getAll<Any?>(jsonPath2) shouldBe listOf()

          jsonArrayOf(jsonObjectOf("b" to true))
            .getAll<Any?>(jsonPath2) shouldBe listOf()
        }

        should("return an error if there are multiple results") {
          val jsonPath1 = JsonPath.compile("$..a").shouldBeRight()
          val jsonPath2 = JsonPath.compile("$[?@.a]").shouldBeRight()

          jsonArrayOf(jsonObjectOf("a" to 2), jsonArrayOf(1, jsonObjectOf("a" to 3)))
            .getAll<Int>(jsonPath1) shouldContainExactly listOf(2, 3)

          jsonArrayOf(
            jsonObjectOf("a" to 1),
            true,
            jsonObjectOf("b" to 3),
            jsonObjectOf("a" to 2),
          ).getAll<JsonObject>(jsonPath2) shouldContainExactly
            listOf(
              jsonObjectOf("a" to 1),
              jsonObjectOf("a" to 2),
            )
        }
      }
    }

    context("The traceOne function") {
      context("applied to a JsonObject") {
        should("return the single result if there is exactly one") {
          val jsonPath1 = JsonPath.compile("$.a").shouldBeRight()
          val jsonPath2 = JsonPath.compile("$.a[1]['b']").shouldBeRight()

          jsonObjectOf("a" to 1, "b" to 2)
            .traceOne(jsonPath1)
            .shouldBeRight() shouldBeSome JsonPath["a"]

          jsonObjectOf("a" to "string")
            .traceOne(jsonPath1)
            .shouldBeRight() shouldBeSome JsonPath["a"]

          jsonObjectOf("a" to jsonArrayOf(null, jsonObjectOf("b" to true)))
            .traceOne(jsonPath2)
            .shouldBeRight() shouldBeSome JsonPath["a"][1]["b"]
        }

        should("return None if there is no result") {
          val jsonPath1 = JsonPath.compile("$.a").shouldBeRight()
          val jsonPath2 = JsonPath.compile("$.a[1]['b']").shouldBeRight()

          jsonPath1.evaluateOne(jsonObjectOf("b" to 2)).shouldBeRight().shouldBeNone()

          jsonObjectOf("b" to jsonObjectOf("a" to 1))
            .traceOne(jsonPath1)
            .shouldBeRight()
            .shouldBeNone()

          jsonObjectOf("b" to jsonArrayOf(null, jsonObjectOf("b" to true)))
            .traceOne(jsonPath2)
            .shouldBeRight()
            .shouldBeNone()

          jsonObjectOf("a" to jsonArrayOf(jsonObjectOf("b" to true)))
            .traceOne(jsonPath2)
            .shouldBeRight()
            .shouldBeNone()

          jsonObjectOf("a" to jsonArrayOf(null, jsonObjectOf("a" to true)))
            .traceOne(jsonPath2)
            .shouldBeRight()
            .shouldBeNone()
        }

        should("return an error if there are multiple results") {
          val jsonPath1 = JsonPath.compile("$..a").shouldBeRight()
          val jsonPath2 = JsonPath.compile("$[?@.a]").shouldBeRight()

          jsonObjectOf(
            "a" to 1,
            "b" to jsonObjectOf("a" to 2),
            "c" to jsonArrayOf(1, jsonObjectOf("a" to 3)),
          ).traceOne(jsonPath1)
            .shouldBeLeft()
            .results
            .shouldContainExactlyInAnyOrder(
              JsonNode(1, JsonPath["a"]),
              JsonNode(2, JsonPath["b"]["a"]),
              JsonNode(3, JsonPath["c"][1]["a"]),
            )

          jsonObjectOf("a" to "string", "b" to jsonObjectOf("a" to null))
            .traceOne(jsonPath1)
            .shouldBeLeft()
            .results
            .shouldContainExactlyInAnyOrder(
              JsonNode("string", JsonPath["a"]),
              JsonNode(null, JsonPath["b"]["a"]),
            )

          jsonObjectOf("a" to jsonObjectOf("a" to 1), "b" to jsonObjectOf("a" to 2))
            .traceOne(jsonPath2)
            .shouldBeLeft()
            .results
            .shouldContainExactlyInAnyOrder(
              JsonNode(jsonObjectOf("a" to 1), JsonPath["a"]),
              JsonNode(jsonObjectOf("a" to 2), JsonPath["b"]),
            )
        }
      }

      context("applied to a JsonArray") {
        should("return the single result if there is exactly one") {
          val jsonPath1 = JsonPath.compile("$[0]").shouldBeRight()
          val jsonPath2 = JsonPath.compile("$[1]['b']").shouldBeRight()

          jsonArrayOf(1, 2, 3)
            .traceOne(jsonPath1)
            .shouldBeRight() shouldBeSome JsonPath[0]

          jsonArrayOf("string", null, true)
            .traceOne(jsonPath1)
            .shouldBeRight() shouldBeSome JsonPath[0]

          jsonArrayOf(null, jsonObjectOf("b" to true))
            .traceOne(jsonPath2)
            .shouldBeRight() shouldBeSome JsonPath[1]["b"]
        }

        should("return None if there is no result") {
          val jsonPath1 = JsonPath.compile("$[0]").shouldBeRight()
          val jsonPath2 = JsonPath.compile("$[1]['b']").shouldBeRight()

          jsonArrayOf()
            .traceOne(jsonPath1)
            .shouldBeRight()
            .shouldBeNone()

          jsonArrayOf(null, jsonObjectOf("a" to true))
            .traceOne(jsonPath2)
            .shouldBeRight()
            .shouldBeNone()

          jsonArrayOf(jsonObjectOf("b" to true))
            .traceOne(jsonPath2)
            .shouldBeRight()
            .shouldBeNone()
        }

        should("return an error if there are multiple results") {
          val jsonPath1 = JsonPath.compile("$..a").shouldBeRight()
          val jsonPath2 = JsonPath.compile("$[?@.a]").shouldBeRight()

          jsonArrayOf(jsonObjectOf("a" to 2), jsonArrayOf(1, jsonObjectOf("a" to 3)))
            .traceOne(jsonPath1)
            .shouldBeLeft()
            .results
            .shouldContainExactly(
              JsonNode(2, JsonPath[0]["a"]),
              JsonNode(3, JsonPath[1][1]["a"]),
            )

          jsonArrayOf(
            jsonObjectOf("a" to 1),
            true,
            jsonObjectOf("b" to 3),
            jsonObjectOf("a" to 2),
          ).traceOne(jsonPath2)
            .shouldBeLeft()
            .results
            .shouldContainExactly(
              JsonNode(jsonObjectOf("a" to 1), JsonPath[0]),
              JsonNode(jsonObjectOf("a" to 2), JsonPath[3]),
            )
        }
      }
    }

    context("The traceAll function") {
      context("applied to a JsonObject") {
        should("return a list containing the single result if there is exactly one") {
          val jsonPath1 = JsonPath.compile("$.a").shouldBeRight()
          val jsonPath2 = JsonPath.compile("$.a[1]['b']").shouldBeRight()

          jsonObjectOf("a" to 1, "b" to 2)
            .traceAll(jsonPath1) shouldBe listOf(JsonPath["a"])

          jsonObjectOf("a" to "string")
            .traceAll(jsonPath1) shouldBe listOf(JsonPath["a"])

          jsonObjectOf("a" to jsonArrayOf(null, jsonObjectOf("b" to true)))
            .traceAll(jsonPath2) shouldBe listOf(JsonPath["a"][1]["b"])
        }

        should("return an empty list if there is no result") {
          val jsonPath1 = JsonPath.compile("$.a").shouldBeRight()
          val jsonPath2 = JsonPath.compile("$.a[1]['b']").shouldBeRight()

          jsonPath1.evaluateOne(jsonObjectOf("b" to 2)).shouldBeRight().shouldBeNone()

          jsonObjectOf("b" to jsonObjectOf("a" to 1))
            .traceAll(jsonPath1) shouldBe listOf()

          jsonObjectOf("b" to jsonArrayOf(null, jsonObjectOf("b" to true)))
            .traceAll(jsonPath2) shouldBe listOf()

          jsonObjectOf("a" to jsonArrayOf(jsonObjectOf("b" to true)))
            .traceAll(jsonPath2) shouldBe listOf()

          jsonObjectOf("a" to jsonArrayOf(null, jsonObjectOf("a" to true)))
            .traceAll(jsonPath2) shouldBe listOf()
        }

        should("return all results if there are multiple") {
          val jsonPath1 = JsonPath.compile("$..a").shouldBeRight()
          val jsonPath2 = JsonPath.compile("$[?@.a]").shouldBeRight()

          jsonObjectOf(
            "a" to 1,
            "b" to jsonObjectOf("a" to 2),
            "c" to jsonArrayOf(1, jsonObjectOf("a" to 3)),
          ).traceAll(jsonPath1) shouldContainExactlyInAnyOrder
            listOf(
              JsonPath["a"],
              JsonPath["b"]["a"],
              JsonPath["c"][1]["a"],
            )

          jsonObjectOf("a" to "string", "b" to jsonObjectOf("a" to null))
            .traceAll(jsonPath1) shouldContainExactlyInAnyOrder
            listOf(
              JsonPath["a"],
              JsonPath["b"]["a"],
            )

          jsonObjectOf("a" to jsonObjectOf("a" to 1), "b" to jsonObjectOf("a" to 2))
            .traceAll(jsonPath2) shouldContainExactlyInAnyOrder
            listOf(
              JsonPath["a"],
              JsonPath["b"],
            )
        }
      }

      context("applied to a JsonArray") {
        should("return the single result if there is exactly one") {
          val jsonPath1 = JsonPath.compile("$[0]").shouldBeRight()
          val jsonPath2 = JsonPath.compile("$[1]['b']").shouldBeRight()

          jsonArrayOf(1, 2, 3)
            .traceAll(jsonPath1) shouldBe listOf(JsonPath[0])

          jsonArrayOf("string", null, true)
            .traceAll(jsonPath1) shouldBe listOf(JsonPath[0])

          jsonArrayOf(null, jsonObjectOf("b" to true))
            .traceAll(jsonPath2) shouldBe listOf(JsonPath[1]["b"])
        }

        should("return None if there is no result") {
          val jsonPath1 = JsonPath.compile("$[0]").shouldBeRight()
          val jsonPath2 = JsonPath.compile("$[1]['b']").shouldBeRight()

          jsonArrayOf()
            .traceAll(jsonPath1) shouldBe listOf()

          jsonArrayOf(null, jsonObjectOf("a" to true))
            .traceAll(jsonPath2) shouldBe listOf()

          jsonArrayOf(jsonObjectOf("b" to true))
            .traceAll(jsonPath2) shouldBe listOf()
        }

        should("return an error if there are multiple results") {
          val jsonPath1 = JsonPath.compile("$..a").shouldBeRight()
          val jsonPath2 = JsonPath.compile("$[?@.a]").shouldBeRight()

          jsonArrayOf(jsonObjectOf("a" to 2), jsonArrayOf(1, jsonObjectOf("a" to 3)))
            .traceAll(jsonPath1) shouldContainExactly listOf(JsonPath[0]["a"], JsonPath[1][1]["a"])

          jsonArrayOf(
            jsonObjectOf("a" to 1),
            true,
            jsonObjectOf("b" to 3),
            jsonObjectOf("a" to 2),
          ).traceAll(jsonPath2) shouldContainExactly
            listOf(
              JsonPath[0],
              JsonPath[3],
            )
        }
      }
    }
  })
