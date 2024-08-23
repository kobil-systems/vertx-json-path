package com.kobil.vertx.jsonpath.error

import com.kobil.vertx.jsonpath.compiler.Token
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.property.checkAll
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

class JsonPathErrorTest :
  ShouldSpec({
    context("The 'invoke' operator of JsonPathError") {
      context("applied to a JsonPathException") {
        should("return the error wrapped in the exception") {
          val errors =
            listOf(
              JsonPathError.UnexpectedError(IllegalArgumentException("a")),
              JsonPathError.UnexpectedError(IllegalStateException("a")),
              JsonPathError.UnexpectedError(NullPointerException("a")),
              JsonPathError.IllegalCharacter('a', 1U, 1U, "something"),
              JsonPathError.UnterminatedString(1U, 1U, 1U, 1U),
              JsonPathError.UnexpectedToken(Token.QuestionMark(1U, 1U), "something"),
              JsonPathError.IllegalSelector(1U, 1U, "something"),
              JsonPathError.UnknownFunction("something", 1U, 1U),
              JsonPathError.MustBeSingularQuery(1U, 1U),
              JsonPathError.InvalidEscapeSequence("abc", 1U, 1U, 1U, "something"),
            )

          for (err in errors) {
            JsonPathError(JsonPathException(err)) shouldBeSameInstanceAs err
          }
        }
      }

      context("applied to a CancellationException") {
        should("rethrow the exception") {
          shouldThrow<CancellationException> {
            JsonPathError(CancellationException("abc"))
          }
        }
      }

      context("applied to an Error") {
        should("rethrow the error") {
          shouldThrow<OutOfMemoryError> {
            JsonPathError(OutOfMemoryError("abc"))
          }
        }
      }

      context("applied to any other exception") {
        should("return an UnexpectedError wrapping the exception") {
          val exceptions =
            listOf(
              IllegalArgumentException("a"),
              IllegalStateException("a"),
              NullPointerException("a"),
              IOException("a"),
            )

          for (ex in exceptions) {
            JsonPathError(ex)
              .shouldBeInstanceOf<JsonPathError.UnexpectedError>()
              .cause shouldBeSameInstanceAs ex
          }
        }
      }
    }

    context("The 'IllegalCharacter' error") {
      should("produce the correct message from toString") {
        checkAll<Char, UInt, UInt, String> { ch, line, col, reason ->
          JsonPathError.IllegalCharacter(ch, line, col, reason).toString() shouldBe
            "Error at [$line:$col]: Illegal character '$ch' ($reason)"
        }
      }
    }

    context("The 'UnterminatedString' error") {
      should("produce the correct message from toString") {
        checkAll<UInt, UInt, UInt, UInt> { line, col, startLine, startCol ->
          JsonPathError.UnterminatedString(line, col, startLine, startCol).toString() shouldBe
            "Error at [$line:$col]: Unterminated string literal starting at [$startLine:$startCol]"
        }
      }
    }

    context("The 'UnexpectedToken' error") {
      should("produce the correct message from toString") {
        checkAll<Token, String> { token, string ->
          JsonPathError.UnexpectedToken(token, string).toString() shouldBe
            "Error at [${token.line}:${token.column}]: Unexpected token '${token.name}' while parsing $string"
        }
      }
    }

    context("The 'IllegalSelector' error") {
      should("produce the correct message from toString") {
        checkAll<UInt, UInt, String> { line, col, string ->
          JsonPathError.IllegalSelector(line, col, string).toString() shouldBe
            "Error at [$line:$col]: Illegal selector ($string)"
        }
      }
    }

    context("The 'UnknownFunction' error") {
      should("produce the correct message from toString") {
        checkAll<UInt, UInt, String> { line, col, name ->
          JsonPathError.UnknownFunction(name, line, col).toString() shouldBe
            "Error at [$line:$col]: Unknown function extension '$name'"
        }
      }
    }

    context("The 'MustBeSingularQuery' error") {
      should("produce the correct message from toString") {
        checkAll<UInt, UInt> { line, col ->
          JsonPathError.MustBeSingularQuery(line, col).toString() shouldBe
            "Error at [$line:$col]: A singular query is expected"
        }
      }
    }

    context("The 'InvalidEscapeSequence' error") {
      should("produce the correct message from toString") {
        checkAll<String, UInt, UInt, UInt, String> { string, line, col, pos, reason ->
          JsonPathError.InvalidEscapeSequence(string, line, col, pos, reason).toString() shouldBe
            "Error at [$line:$col]: Invalid escape sequence at position $pos in string literal '$string' ($reason)"
        }
      }
    }
  })
