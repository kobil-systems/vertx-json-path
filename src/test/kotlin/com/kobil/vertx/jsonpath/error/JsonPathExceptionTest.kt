package com.kobil.vertx.jsonpath.error

import com.kobil.vertx.jsonpath.compiler.Token
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.throwable.shouldHaveCause
import io.kotest.matchers.throwable.shouldHaveMessage
import io.kotest.matchers.throwable.shouldNotHaveCause
import io.kotest.matchers.types.shouldBeSameInstanceAs

class JsonPathExceptionTest :
  ShouldSpec({
    context("The constructor of JsonPathException") {
      context("called on an instance of UnexpectedError") {
        should("use the cause of the UnexpectedError as cause") {
          checkUnexpectedErrorCause(IllegalArgumentException("a"))
          checkUnexpectedErrorCause(IllegalStateException("a"))
          checkUnexpectedErrorCause(NullPointerException("a"))
        }

        should("have a message of 'Unexpected error: <exception>'") {
          checkUnexpectedErrorMessage(IllegalArgumentException("a"))
          checkUnexpectedErrorMessage(IllegalStateException("a"))
          checkUnexpectedErrorMessage(NullPointerException("a"))
        }

        should("have no stack trace") {
          checkNoStackTrace(JsonPathError.UnexpectedError(IllegalArgumentException("a")))
          checkNoStackTrace(JsonPathError.UnexpectedError(IllegalStateException("a")))
          checkNoStackTrace(JsonPathError.UnexpectedError(NullPointerException("a")))
        }
      }

      context("called on any instance of JsonPathError that is no UnexpectedError") {
        should("have no cause") {
          checkErrorNoCause(JsonPathError.IllegalCharacter('a', 1U, 1U, "something"))
          checkErrorNoCause(JsonPathError.UnterminatedString(1U, 1U, 1U, 1U))
          checkErrorNoCause(JsonPathError.UnexpectedToken(Token.QuestionMark(1U, 1U), "something"))
          checkErrorNoCause(JsonPathError.IllegalSelector(1U, 1U, "something"))
          checkErrorNoCause(JsonPathError.UnknownFunction("something", 1U, 1U))
          checkErrorNoCause(JsonPathError.MustBeSingularQuery(1U, 1U))
          checkErrorNoCause(JsonPathError.InvalidEscapeSequence("abc", 1U, 1U, 1U, "something"))
        }

        should("have a message of error.toString") {
          checkErrorMessage(JsonPathError.IllegalCharacter('a', 1U, 1U, "something"))
          checkErrorMessage(JsonPathError.UnterminatedString(1U, 1U, 1U, 1U))
          checkErrorMessage(JsonPathError.UnexpectedToken(Token.QuestionMark(1U, 1U), "something"))
          checkErrorMessage(JsonPathError.IllegalSelector(1U, 1U, "something"))
          checkErrorMessage(JsonPathError.UnknownFunction("something", 1U, 1U))
          checkErrorMessage(JsonPathError.MustBeSingularQuery(1U, 1U))
          checkErrorMessage(JsonPathError.InvalidEscapeSequence("abc", 1U, 1U, 1U, "something"))
        }

        should("have no stack trace") {
          checkNoStackTrace(JsonPathError.IllegalCharacter('a', 1U, 1U, "something"))
          checkNoStackTrace(JsonPathError.UnterminatedString(1U, 1U, 1U, 1U))
          checkNoStackTrace(JsonPathError.UnexpectedToken(Token.QuestionMark(1U, 1U), "something"))
          checkNoStackTrace(JsonPathError.IllegalSelector(1U, 1U, "something"))
          checkNoStackTrace(JsonPathError.UnknownFunction("something", 1U, 1U))
          checkNoStackTrace(JsonPathError.MustBeSingularQuery(1U, 1U))
          checkNoStackTrace(JsonPathError.InvalidEscapeSequence("abc", 1U, 1U, 1U, "something"))
        }
      }
    }
  })

private fun checkUnexpectedErrorCause(t: Throwable) {
  val e = JsonPathException(JsonPathError.UnexpectedError(t))
  e.shouldHaveCause { it shouldBeSameInstanceAs t }
}

private fun checkUnexpectedErrorMessage(t: Throwable) {
  val e = JsonPathException(JsonPathError.UnexpectedError(t))
  e shouldHaveMessage "Unexpected error: $t"
}

private fun checkErrorNoCause(e: JsonPathError) {
  JsonPathException(e).shouldNotHaveCause()
}

private fun checkErrorMessage(e: JsonPathError) {
  JsonPathException(e) shouldHaveMessage e.toString()
}

private fun checkNoStackTrace(e: JsonPathError) {
  JsonPathException(e).stackTrace.shouldBeEmpty()
}
