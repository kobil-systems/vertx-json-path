package com.kobil.vertx.jsonpath

import com.kobil.vertx.jsonpath.JsonPath.Companion.onlyValues
import com.kobil.vertx.jsonpath.testing.ShouldSpecContext
import com.kobil.vertx.jsonpath.testing.VertxExtension
import com.kobil.vertx.jsonpath.testing.VertxExtension.Companion.vertx
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.fail
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.shouldBe
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.get
import io.vertx.kotlin.coroutines.coAwait

class ComplianceTest :
  ShouldSpec({
    println("Instantiating test suite")

    register(VertxExtension())

    context("The JSON Path implementation should pass the standard compliance test suite") {
      val testSuite =
        vertx
          .fileSystem()
          .readFile("compliance-test-suite/cts.json")
          .coAwait()
          .toJsonObject()

      val tests =
        testSuite
          .getJsonArray("tests")
          .filterIsInstance<JsonObject>()

      for (test in tests) {
        val ctx: ShouldSpecContext =
          if (test.name in skippedTests) {
            { name, block -> xcontext(name, block) }
          } else {
            { name, block -> context(name, block) }
          }

        if (test.isInvalid) {
          ctx(test.name) {
            should("yield an error when compiled") {
              JsonPath.compile(vertx, test.selector).shouldBeLeft()
            }
          }
        } else {
          val result = test.result?.toList()
          val results = test.results?.map { (it as JsonArray).toList() }

          if (result != null) {
            ctx(test.name) {
              should("compile to a valid JSON path") {
                JsonPath.compile(vertx, test.selector).shouldBeRight()
              }

              should("yield the expected results") {
                val jsonPath = JsonPath.compile(vertx, test.selector).shouldBeRight()

                val actual =
                  when (val d = test.document) {
                    is JsonObject -> jsonPath.evaluate(d).onlyValues()
                    is JsonArray -> jsonPath.evaluate(d).onlyValues()
                    else -> fail("Unsupported document $d")
                  }

                actual shouldBe result
              }
            }
          } else if (results != null) {
            ctx(test.name) {
              should("compile to a valid JSON path") {
                JsonPath.compile(vertx, test.selector).shouldBeRight()
              }

              should("yield the expected results") {
                val jsonPath = JsonPath.compile(vertx, test.selector).shouldBeRight()

                val actual =
                  when (val d = test.document) {
                    is JsonObject -> jsonPath.evaluate(d).onlyValues()
                    is JsonArray -> jsonPath.evaluate(d).onlyValues()
                    else -> fail("Unsupported document $d")
                  }

                actual shouldBeIn results
              }
            }
          } else {
            fail(
              "Unsupported test case: invalid_selector=false, but neither result nor results is present",
            )
          }
        }
      }
    }
  }) {
  companion object {
    val skippedTests =
      setOf(
        // Tests using values greater than Java's int, which makes no sense in Java as
        // indices can never go beyond int's max value
        "index selector, min exact index",
        "index selector, max exact index",
        "slice selector, excessively large to value",
        "slice selector, excessively small from value",
        "slice selector, excessively large from value with negative step",
        "slice selector, excessively small to value with negative step",
        "slice selector, excessively large step",
        "slice selector, excessively small step",
        "slice selector, start, min exact",
        "slice selector, start, max exact",
        "slice selector, end, min exact",
        "slice selector, end, max exact",
        "slice selector, step, min exact",
        "slice selector, step, max exact",
      )

    private val JsonObject.name: String
      get() = this["name"]

    private val JsonObject.selector: String
      get() = this["selector"]

    private val JsonObject.document: Any?
      get() = this["document"]

    private val JsonObject.result: JsonArray?
      get() = this["result"]

    private val JsonObject.results: JsonArray?
      get() = this["results"]

    private val JsonObject.isInvalid: Boolean
      get() = getBoolean("invalid_selector", false)
  }
}
