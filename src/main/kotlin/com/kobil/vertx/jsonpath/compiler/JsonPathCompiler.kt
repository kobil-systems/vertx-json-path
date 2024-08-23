package com.kobil.vertx.jsonpath.compiler

import arrow.core.Either
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.kobil.vertx.jsonpath.FilterExpression
import com.kobil.vertx.jsonpath.JsonPath
import com.kobil.vertx.jsonpath.error.JsonPathError
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

object JsonPathCompiler {
  private val queryCache: Cache<String, Either<JsonPathError, JsonPath>> =
    Caffeine
      .newBuilder()
      .maximumSize(1_000)
      .build()

  private val filterCache: Cache<String, Either<JsonPathError, FilterExpression>> =
    Caffeine
      .newBuilder()
      .maximumSize(1_000)
      .build()

  suspend fun Vertx.compileJsonPathQuery(jsonPath: String): Either<JsonPathError, JsonPath> =
    queryCache
      .getIfPresent(jsonPath)
      ?: launchInScope {
        jsonPath.scanTokens().parseJsonPathQuery()
      }.also { queryCache.put(jsonPath, it) }

  suspend fun Vertx.compileJsonPathFilter(
    filterExpression: String,
  ): Either<JsonPathError, FilterExpression> =
    filterCache
      .getIfPresent(filterExpression)
      ?: launchInScope {
        filterExpression.scanTokens().parseJsonPathFilter()
      }.also { filterCache.put(filterExpression, it) }

  private suspend inline fun <T> Vertx.launchInScope(
    crossinline block: suspend CoroutineScope.() -> T,
  ): T =
    coroutineScope {
      async(dispatcher()) { block() }.await()
    }
}
