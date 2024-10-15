package com.kobil.vertx.jsonpath.compiler

import arrow.core.Either
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import com.kobil.vertx.jsonpath.FilterExpression
import com.kobil.vertx.jsonpath.JsonPath
import com.kobil.vertx.jsonpath.error.JsonPathError

/**
 * An interface to the JSON Path compiler, caching results of compilation request. It backs the
 * implementation of [JsonPath.compile] and [FilterExpression.compile].
 */
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

  /**
   * Compiles a filter expression string. It will return an instance of [Either.Right] containing
   * the compiled [JsonPath] if the compilation was successful. Otherwise, an error wrapped in an
   * instance of [Either.Left] is returned.
   *
   * The result of up to 1000 compilations is cached for performance reasons.
   *
   * @param jsonPath the JSON Path string
   * @return the compiled [JsonPath], or an error
   *
   * @see JsonPath.compile
   * @see JsonPathError
   */
  @JvmStatic
  fun compileJsonPathQuery(jsonPath: String): Either<JsonPathError, JsonPath> =
    queryCache.get(jsonPath)

  /**
   * Compiles a filter expression string. It will return an instance of [Either.Right] containing
   * the compiled expression if the compilation was successful. Otherwise, an error wrapped in an
   * instance of [Either.Left] is returned.
   *
   * Filter expression strings must not have a leading `?`.
   *
   * The result of up to 1000 compilations is cached for performance reasons.
   *
   * @param filterExpression the filter expression string without leading `?`
   * @return the compiled filter expression, or an error
   *
   * @see FilterExpression.compile
   * @see JsonPathError
   */
  @JvmStatic
  fun compileJsonPathFilter(filterExpression: String): Either<JsonPathError, FilterExpression> =
    filterCache.get(filterExpression)
}
