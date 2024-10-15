package com.kobil.vertx.jsonpath.testing

import io.kotest.assertions.fail
import io.kotest.core.extensions.SpecExtension
import io.kotest.core.extensions.TestCaseExtension
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.scopes.ContainerScope
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.kotest.core.test.TestScope
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

class VertxExtension(
  private val lifecycle: Lifecycle = Lifecycle.Spec,
) : TestCaseExtension,
  SpecExtension {
  companion object {
    val TestScope.vertx: Vertx
      get() =
        coroutineContext[VertxKey]?.vertx
          ?: fail("VertxExtension not installed for this test")
    val ContainerScope.vertx: Vertx
      get() =
        coroutineContext[VertxKey]?.vertx
          ?: fail("VertxExtension not installed for this test")
  }

  override suspend fun intercept(
    testCase: TestCase,
    execute: suspend (TestCase) -> TestResult,
  ): TestResult {
    if (lifecycle == Lifecycle.Test) {
      val vertx = Vertx.vertx()

      val job =
        CoroutineScope(vertx.dispatcher() + VertxElement(vertx)).async {
          execute(testCase)
        }

      val result = job.await()

      vertx.close().coAwait()

      return result
    } else {
      return execute(testCase)
    }
  }

  override suspend fun intercept(
    spec: Spec,
    execute: suspend (Spec) -> Unit,
  ) {
    if (lifecycle == Lifecycle.Spec) {
      val vertx = Vertx.vertx()

      val job =
        CoroutineScope(vertx.dispatcher() + VertxElement(vertx)).launch {
          execute(spec)
        }

      job.join()

      vertx.close().coAwait()
    } else {
      execute(spec)
    }
  }

  enum class Lifecycle {
    Spec,
    Test,
  }

  private data object VertxKey : CoroutineContext.Key<VertxElement>

  private data class VertxElement(
    val vertx: Vertx,
  ) : AbstractCoroutineContextElement(VertxKey)
}
