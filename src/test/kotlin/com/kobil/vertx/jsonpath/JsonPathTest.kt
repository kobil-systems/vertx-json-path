package com.kobil.vertx.jsonpath

import com.kobil.vertx.jsonpath.FilterExpression.Existence
import com.kobil.vertx.jsonpath.JsonPath.Companion.onlyPaths
import com.kobil.vertx.jsonpath.QueryExpression.Relative
import com.kobil.vertx.jsonpath.Segment.ChildSegment
import com.kobil.vertx.jsonpath.Segment.DescendantSegment
import com.kobil.vertx.jsonpath.Selector.Index
import com.kobil.vertx.jsonpath.Selector.Name
import com.kobil.vertx.jsonpath.error.MultipleResults
import com.kobil.vertx.jsonpath.error.RequiredJsonValueError.NoResult
import com.kobil.vertx.jsonpath.testing.normalizedJsonPath
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeNone
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.arrow.core.shouldBeSome
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.orNull
import io.kotest.property.arbitrary.string
import io.kotest.property.arbitrary.triple
import io.kotest.property.checkAll
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.jsonArrayOf
import io.vertx.kotlin.core.json.jsonObjectOf

class JsonPathTest :
  ShouldSpec({
    context("The evaluateOne function") {
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

    context("The getAll function") {
      context("called on a JSON object") {
        should("return the single result if there is exactly one") {
          val jsonPath1 = JsonPath.compile("$.a").shouldBeRight()
          val jsonPath2 = JsonPath.compile("$.a[1]['b']").shouldBeRight()

          jsonPath1
            .getAll<Int>(jsonObjectOf("a" to 1, "b" to 2))
            .shouldContainExactly(1)

          jsonPath1
            .getAll<String>(jsonObjectOf("a" to "string"))
            .shouldContainExactly("string")

          jsonPath2
            .getAll<Boolean>(jsonObjectOf("a" to jsonArrayOf(null, jsonObjectOf("b" to true))))
            .shouldContainExactly(true)
        }

        should("return an empty list if there is no result") {
          val jsonPath1 = JsonPath.compile("$.a").shouldBeRight()
          val jsonPath2 = JsonPath.compile("$.a[1]['b']").shouldBeRight()

          jsonPath1.evaluateOne(jsonObjectOf("b" to 2)).shouldBeRight().shouldBeNone()

          jsonPath1
            .getAll<Any?>(jsonObjectOf("b" to jsonObjectOf("a" to 1)))
            .shouldBeEmpty()

          jsonPath2
            .getAll<Any?>(jsonObjectOf("b" to jsonArrayOf(null, jsonObjectOf("b" to true))))
            .shouldBeEmpty()

          jsonPath2
            .getAll<Any?>(jsonObjectOf("a" to jsonArrayOf(jsonObjectOf("b" to true))))
            .shouldBeEmpty()

          jsonPath2
            .getAll<Any?>(jsonObjectOf("a" to jsonArrayOf(null, jsonObjectOf("a" to true))))
            .shouldBeEmpty()
        }

        should("return all results if there are multiple") {
          val jsonPath1 = JsonPath.compile("$..a").shouldBeRight()
          val jsonPath2 = JsonPath.compile("$[?@.a]").shouldBeRight()

          jsonPath1
            .getAll<Int>(
              jsonObjectOf(
                "a" to 1,
                "b" to jsonObjectOf("a" to 2),
                "c" to jsonArrayOf(1, jsonObjectOf("a" to 3)),
              ),
            ).shouldContainExactlyInAnyOrder(1, 2, 3)

          jsonPath1
            .getAll<String?>(jsonObjectOf("a" to "string", "b" to jsonObjectOf("a" to null)))
            .shouldContainExactlyInAnyOrder("string", null)

          jsonPath2
            .getAll<JsonObject>(
              jsonObjectOf(
                "a" to jsonObjectOf("a" to 1),
                "b" to jsonObjectOf("a" to 2),
              ),
            ).shouldContainExactlyInAnyOrder(
              jsonObjectOf("a" to 1),
              jsonObjectOf("a" to 2),
            )
        }
      }

      context("called on a JSON array") {
        should("return the single result if there is exactly one") {
          val jsonPath1 = JsonPath.compile("$[0]").shouldBeRight()
          val jsonPath2 = JsonPath.compile("$[1]['b']").shouldBeRight()

          jsonPath1
            .getAll<Int>(jsonArrayOf(1, 2, 3))
            .shouldContainExactly(1)

          jsonPath1
            .getAll<String>(jsonArrayOf("string", null, true))
            .shouldContainExactly("string")

          jsonPath2
            .getAll<Boolean>(jsonArrayOf(null, jsonObjectOf("b" to true)))
            .shouldContainExactly(true)
        }

        should("return an empty list if there is no result") {
          val jsonPath1 = JsonPath.compile("$[0]").shouldBeRight()
          val jsonPath2 = JsonPath.compile("$[1]['b']").shouldBeRight()

          jsonPath1.evaluateOne(jsonArrayOf()).shouldBeRight().shouldBeNone()

          jsonPath2
            .getAll<Any?>(jsonArrayOf(null, jsonObjectOf("a" to true)))
            .shouldBeEmpty()

          jsonPath2
            .getAll<Any?>(jsonArrayOf(jsonObjectOf("b" to true)))
            .shouldBeEmpty()
        }

        should("return all results if there are multiple") {
          val jsonPath1 = JsonPath.compile("$..a").shouldBeRight()
          val jsonPath2 = JsonPath.compile("$[?@.a]").shouldBeRight()

          jsonPath1
            .getAll<Int>(
              jsonArrayOf(
                jsonObjectOf("a" to 2),
                jsonArrayOf(1, jsonObjectOf("a" to 3)),
              ),
            ).shouldContainExactly(2, 3)

          jsonPath2
            .getAll<JsonObject>(
              jsonArrayOf(
                jsonObjectOf("a" to 1),
                true,
                jsonObjectOf("b" to 3),
                jsonObjectOf("a" to 2),
              ),
            ).shouldContainExactly(
              jsonObjectOf("a" to 1),
              jsonObjectOf("a" to 2),
            )
        }
      }
    }

    context("The getOne function") {
      context("called on a JSON object") {
        should("return the single result if there is exactly one") {
          val jsonPath1 = JsonPath.compile("$.a").shouldBeRight()
          val jsonPath2 = JsonPath.compile("$.a[1]['b']").shouldBeRight()

          jsonPath1
            .getOne<Int>(jsonObjectOf("a" to 1, "b" to 2))
            .shouldBeRight() shouldBeSome 1

          jsonPath1
            .getOne<String>(jsonObjectOf("a" to "string"))
            .shouldBeRight() shouldBeSome "string"

          jsonPath2
            .getOne<Boolean>(jsonObjectOf("a" to jsonArrayOf(null, jsonObjectOf("b" to true))))
            .shouldBeRight() shouldBeSome true
        }

        should("return None if there is no result") {
          val jsonPath1 = JsonPath.compile("$.a").shouldBeRight()
          val jsonPath2 = JsonPath.compile("$.a[1]['b']").shouldBeRight()

          jsonPath1.getOne<Any?>(jsonObjectOf("b" to 2)).shouldBeRight().shouldBeNone()

          jsonPath1
            .getOne<Any?>(jsonObjectOf("b" to jsonObjectOf("a" to 1)))
            .shouldBeRight()
            .shouldBeNone()

          jsonPath2
            .getOne<Any?>(jsonObjectOf("b" to jsonArrayOf(null, jsonObjectOf("b" to true))))
            .shouldBeRight()
            .shouldBeNone()

          jsonPath2
            .getOne<Any?>(jsonObjectOf("a" to jsonArrayOf(jsonObjectOf("b" to true))))
            .shouldBeRight()
            .shouldBeNone()

          jsonPath2
            .getOne<Any?>(jsonObjectOf("a" to jsonArrayOf(null, jsonObjectOf("a" to true))))
            .shouldBeRight()
            .shouldBeNone()
        }

        should("return an error if there are multiple results") {
          val jsonPath1 = JsonPath.compile("$..a").shouldBeRight()
          val jsonPath2 = JsonPath.compile("$[?@.a]").shouldBeRight()

          jsonPath1
            .getOne<Any?>(
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
            .getOne<Any?>(jsonObjectOf("a" to "string", "b" to jsonObjectOf("a" to null)))
            .shouldBeLeft()
            .results
            .shouldContainExactlyInAnyOrder(
              JsonNode("string", JsonPath["a"]),
              JsonNode(null, JsonPath["b"]["a"]),
            )

          jsonPath2
            .getOne<Any?>(
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
            .getOne<Int>(jsonArrayOf(1, 2, 3))
            .shouldBeRight() shouldBeSome 1

          jsonPath1
            .getOne<String>(jsonArrayOf("string", null, true))
            .shouldBeRight() shouldBeSome "string"

          jsonPath2
            .getOne<Boolean>(jsonArrayOf(null, jsonObjectOf("b" to true)))
            .shouldBeRight() shouldBeSome true
        }

        should("return None if there is no result") {
          val jsonPath1 = JsonPath.compile("$[0]").shouldBeRight()
          val jsonPath2 = JsonPath.compile("$[1]['b']").shouldBeRight()

          jsonPath1.getOne<Any?>(jsonArrayOf()).shouldBeRight().shouldBeNone()

          jsonPath2
            .getOne<Any?>(jsonArrayOf(null, jsonObjectOf("a" to true)))
            .shouldBeRight()
            .shouldBeNone()

          jsonPath2
            .getOne<Any?>(jsonArrayOf(jsonObjectOf("b" to true)))
            .shouldBeRight()
            .shouldBeNone()
        }

        should("return an error if there are multiple results") {
          val jsonPath1 = JsonPath.compile("$..a").shouldBeRight()
          val jsonPath2 = JsonPath.compile("$[?@.a]").shouldBeRight()

          jsonPath1
            .getOne<Any?>(
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
            .getOne<Any?>(
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

    context("The requireOne function") {
      context("called on a JSON object") {
        should("return the single result if there is exactly one") {
          val jsonPath1 = JsonPath.compile("$.a").shouldBeRight()
          val jsonPath2 = JsonPath.compile("$.a[1]['b']").shouldBeRight()

          jsonPath1
            .requireOne<Int>(jsonObjectOf("a" to 1, "b" to 2)) shouldBeRight 1

          jsonPath1
            .requireOne<String>(jsonObjectOf("a" to "string")) shouldBeRight "string"

          jsonPath2
            .requireOne<Boolean>(jsonObjectOf("a" to jsonArrayOf(null, jsonObjectOf("b" to true))))
            .shouldBeRight(true)
        }

        should("return None if there is no result") {
          val jsonPath1 = JsonPath.compile("$.a").shouldBeRight()
          val jsonPath2 = JsonPath.compile("$.a[1]['b']").shouldBeRight()

          jsonPath1.requireOne<Any?>(jsonObjectOf("b" to 2)) shouldBeLeft NoResult

          jsonPath1
            .requireOne<Any?>(jsonObjectOf("b" to jsonObjectOf("a" to 1))) shouldBeLeft NoResult

          jsonPath2
            .requireOne<Any?>(
              jsonObjectOf(
                "b" to
                  jsonArrayOf(
                    null,
                    jsonObjectOf("b" to true),
                  ),
              ),
            ) shouldBeLeft NoResult

          jsonPath2
            .requireOne<Any?>(
              jsonObjectOf("a" to jsonArrayOf(jsonObjectOf("b" to true))),
            ) shouldBeLeft
            NoResult

          jsonPath2
            .requireOne<Any?>(
              jsonObjectOf(
                "a" to
                  jsonArrayOf(
                    null,
                    jsonObjectOf("a" to true),
                  ),
              ),
            ) shouldBeLeft NoResult
        }

        should("return an error if there are multiple results") {
          val jsonPath1 = JsonPath.compile("$..a").shouldBeRight()
          val jsonPath2 = JsonPath.compile("$[?@.a]").shouldBeRight()

          jsonPath1
            .requireOne<Any?>(
              jsonObjectOf(
                "a" to 1,
                "b" to jsonObjectOf("a" to 2),
                "c" to jsonArrayOf(1, jsonObjectOf("a" to 3)),
              ),
            ).shouldBeLeft()
            .shouldBeInstanceOf<MultipleResults>()
            .results
            .shouldContainExactlyInAnyOrder(
              JsonNode(1, JsonPath["a"]),
              JsonNode(2, JsonPath["b"]["a"]),
              JsonNode(3, JsonPath["c"][1]["a"]),
            )

          jsonPath1
            .requireOne<Any?>(jsonObjectOf("a" to "string", "b" to jsonObjectOf("a" to null)))
            .shouldBeLeft()
            .shouldBeInstanceOf<MultipleResults>()
            .results
            .shouldContainExactlyInAnyOrder(
              JsonNode("string", JsonPath["a"]),
              JsonNode(null, JsonPath["b"]["a"]),
            )

          jsonPath2
            .requireOne<Any?>(
              jsonObjectOf(
                "a" to jsonObjectOf("a" to 1),
                "b" to jsonObjectOf("a" to 2),
              ),
            ).shouldBeLeft()
            .shouldBeInstanceOf<MultipleResults>()
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
            .requireOne<Int>(jsonArrayOf(1, 2, 3)) shouldBeRight 1

          jsonPath1
            .requireOne<String>(jsonArrayOf("string", null, true)) shouldBeRight "string"

          jsonPath2
            .requireOne<Boolean>(jsonArrayOf(null, jsonObjectOf("b" to true))) shouldBeRight true
        }

        should("return None if there is no result") {
          val jsonPath1 = JsonPath.compile("$[0]").shouldBeRight()
          val jsonPath2 = JsonPath.compile("$[1]['b']").shouldBeRight()

          jsonPath1.requireOne<Any?>(jsonArrayOf()) shouldBeLeft NoResult

          jsonPath2
            .requireOne<Any?>(jsonArrayOf(null, jsonObjectOf("a" to true))) shouldBeLeft NoResult

          jsonPath2
            .requireOne<Any?>(jsonArrayOf(jsonObjectOf("b" to true))) shouldBeLeft NoResult
        }

        should("return an error if there are multiple results") {
          val jsonPath1 = JsonPath.compile("$..a").shouldBeRight()
          val jsonPath2 = JsonPath.compile("$[?@.a]").shouldBeRight()

          jsonPath1
            .requireOne<Any?>(
              jsonArrayOf(
                jsonObjectOf("a" to 2),
                jsonArrayOf(1, jsonObjectOf("a" to 3)),
              ),
            ).shouldBeLeft()
            .shouldBeInstanceOf<MultipleResults>()
            .results
            .shouldContainExactlyInAnyOrder(
              JsonNode(2, JsonPath[0]["a"]),
              JsonNode(3, JsonPath[1][1]["a"]),
            )

          jsonPath2
            .requireOne<Any?>(
              jsonArrayOf(
                jsonObjectOf("a" to 1),
                true,
                jsonObjectOf("b" to 3),
                jsonObjectOf("a" to 2),
              ),
            ).shouldBeLeft()
            .shouldBeInstanceOf<MultipleResults>()
            .results
            .shouldContainExactlyInAnyOrder(
              JsonNode(jsonObjectOf("a" to 1), JsonPath[0]),
              JsonNode(jsonObjectOf("a" to 2), JsonPath[3]),
            )
        }
      }
    }

    context("The plus operator") {
      context("specifying a single segment") {
        should("append the segment") {
          val n = Selector.name("a")
          val i = Selector.index(1)
          val s = Selector.slice(1, -1, 2)
          val w = Selector.Wildcard
          val f = Selector.filter(QueryExpression.absolute().exists())

          (JsonPath.ROOT + ChildSegment(n)).segments shouldBe
            listOf(
              ChildSegment(n),
            )

          (JsonPath.ROOT + ChildSegment(i, f)).segments shouldBe
            listOf(
              ChildSegment(i, f),
            )

          (
            JsonPath.ROOT + DescendantSegment(n) + ChildSegment(w) +
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

            (JsonPath.ROOT + segments).segments shouldBe segments
            (JsonPath.ROOT + ChildSegment(n) + segments).segments shouldBe
              listOf(ChildSegment(n)) + segments
          }
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

    context("The static get operator") {
      context("applied to one or more strings") {
        should("append a child segment selecting the specified field names") {
          checkAll(Arb.string(), Arb.string(), Arb.string()) { a, b, c ->
            JsonPath[a].segments shouldBe
              listOf(
                ChildSegment(Name(a)),
              )

            JsonPath[a, b].segments shouldBe
              listOf(
                ChildSegment(listOf(Name(a), Name(b))),
              )

            JsonPath[a, c].segments shouldBe
              listOf(
                ChildSegment(listOf(Name(a), Name(c))),
              )

            JsonPath[a, b, c].segments shouldBe
              listOf(
                ChildSegment(listOf(Name(a), Name(b), Name(c))),
              )

            JsonPath[a, c][b].segments shouldBe
              listOf(
                ChildSegment(listOf(Name(a), Name(c))),
                ChildSegment(listOf(Name(b))),
              )
          }
        }
      }

      context("applied to one or more integers") {
        should("append a child segment selecting the specified indices") {
          checkAll(Arb.int(), Arb.int(), Arb.int()) { a, b, c ->
            JsonPath[a].segments shouldBe
              listOf(
                ChildSegment(Index(a)),
              )

            JsonPath[a, b].segments shouldBe
              listOf(
                ChildSegment(listOf(Index(a), Index(b))),
              )

            JsonPath[a, c].segments shouldBe
              listOf(
                ChildSegment(listOf(Index(a), Index(c))),
              )

            JsonPath[a, b, c].segments shouldBe
              listOf(
                ChildSegment(listOf(Index(a), Index(b), Index(c))),
              )

            JsonPath[a, c][b].segments shouldBe
              listOf(
                ChildSegment(listOf(Index(a), Index(c))),
                ChildSegment(listOf(Index(b))),
              )
          }
        }
      }

      context("applied to one or more selectors") {
        should("append a child segment using the specified selectors") {
          checkAll(
            Arb.string(),
            Arb.int(),
            Arb.triple(Arb.int().orNull(), Arb.int().orNull(), Arb.int().orNull()),
          ) { name, idx, (first, last, step) ->
            val n = Selector.name(name)
            val i = Selector.index(idx)
            val w = Selector.Wildcard
            val s = Selector.slice(first, last, step)
            val f = Selector.filter(Existence(Relative()["a"]))

            JsonPath[n].segments shouldBe
              listOf(
                ChildSegment(n),
              )

            JsonPath[n, i].segments shouldBe
              listOf(
                ChildSegment(listOf(n, i)),
              )

            JsonPath[n, i, s].segments shouldBe
              listOf(
                ChildSegment(listOf(n, i, s)),
              )

            JsonPath[n, i][s, w].segments shouldBe
              listOf(
                ChildSegment(listOf(n, i)),
                ChildSegment(listOf(s, w)),
              )

            JsonPath[n, i, f][w][i, f, s].segments shouldBe
              listOf(
                ChildSegment(listOf(n, i, f)),
                ChildSegment(listOf(w)),
                ChildSegment(listOf(i, f, s)),
              )
          }
        }
      }
    }
  })
