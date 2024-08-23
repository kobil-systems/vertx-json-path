package com.kobil.vertx.jsonpath.compiler

import arrow.core.Either
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import com.kobil.vertx.jsonpath.FilterExpression
import com.kobil.vertx.jsonpath.JsonPath
import com.kobil.vertx.jsonpath.error.JsonPathError

object JsonPathCompiler {
  private val queryCache: LoadingCache<String, Either<JsonPathError, JsonPath>> =
    Caffeine
      .newBuilder()
      .maximumSize(1_000)
      .build { jsonPath -> jsonPath.scanTokens().parseJsonPathQuery() }

  private val filterCache: LoadingCache<String, Either<JsonPathError, FilterExpression>> =
    Caffeine
      .newBuilder()
      .maximumSize(1_000)
      .build { filterExpression -> filterExpression.scanTokens().parseJsonPathFilter() }

  fun compileJsonPathQuery(jsonPath: String): Either<JsonPathError, JsonPath> =
    queryCache.get(jsonPath)

  fun compileJsonPathFilter(filterExpression: String): Either<JsonPathError, FilterExpression> =
    filterCache.get(filterExpression)
}
