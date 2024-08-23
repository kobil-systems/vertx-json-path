package com.kobil.vertx.jsonpath.compiler

import com.kobil.vertx.jsonpath.testing.nameChar
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.az
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.flow.toList

class JsonPathScannerTest :
  ShouldSpec({
    context("When called on an empty string") {
      should("only return an EOF token") {
        "".scanTokens().toList() shouldBe listOf(Token.Eof(1U, 1U))
      }
    }

    context("Dotted segments") {
      should("be parsed to an Identifier token when they start with a lowercase latin letter") {
        checkAll(
          Arb.string(1, Codepoint.az()),
          Arb.string(1..48, Codepoint.nameChar()),
        ) { firstLetter, rest ->
          val name = "$firstLetter$rest"

          "$.$name".scanTokens().toList() shouldBe
            listOf(
              Token.Dollar(1U, 1U),
              Token.Dot(1U, 2U),
              Token.Identifier(1U, 3U, name),
              Token.Eof(1U, name.length.toUInt() + 3U),
            )
        }
      }
      should("be parsed to an Identifier token when they start with a uppercase latin letter") {
        checkAll(
          Arb.string(1, Arb.of(('A'.code..'Z'.code).map(::Codepoint))),
          Arb.string(1..48, Codepoint.nameChar()),
        ) { firstLetter, rest ->
          val name = "$firstLetter$rest"

          "$.$name".scanTokens().toList() shouldBe
            listOf(
              Token.Dollar(1U, 1U),
              Token.Dot(1U, 2U),
              Token.Identifier(1U, 3U, name),
              Token.Eof(1U, name.length.toUInt() + 3U),
            )
        }
      }

      should("be parsed to an Identifier token when they start with an underscore") {
        checkAll(Arb.string(1..48, Codepoint.nameChar())) { rest ->
          val name = "_$rest"

          "$.$name".scanTokens().toList() shouldBe
            listOf(
              Token.Dollar(1U, 1U),
              Token.Dot(1U, 2U),
              Token.Identifier(1U, 3U, name),
              Token.Eof(1U, name.length.toUInt() + 3U),
            )
        }
      }

      should("be parsed to an Identifier token when they start with a letter U+0080-U+D7FF") {
        checkAll(
          Arb.string(1, Arb.of((0x80..0xd7ff).map(::Codepoint))),
          Arb.string(1..48, Codepoint.nameChar()),
        ) { firstLetter, rest ->
          val name = "$firstLetter$rest"

          "$.$name".scanTokens().toList() shouldBe
            listOf(
              Token.Dollar(1U, 1U),
              Token.Dot(1U, 2U),
              Token.Identifier(1U, 3U, name),
              Token.Eof(1U, name.length.toUInt() + 3U),
            )
        }
      }

      should("be parsed to an Identifier token when they start with a letter U+E000-U+FFFF") {
        checkAll(
          Arb.string(1, Arb.of((0xe000..0xffff).map(::Codepoint))),
          Arb.string(1..48, Codepoint.nameChar()),
        ) { firstLetter, rest ->
          val name = "$firstLetter$rest"

          "$.$name".scanTokens().toList() shouldBe
            listOf(
              Token.Dollar(1U, 1U),
              Token.Dot(1U, 2U),
              Token.Identifier(1U, 3U, name),
              Token.Eof(1U, name.length.toUInt() + 3U),
            )
        }
      }
    }
  })
