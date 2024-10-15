package com.kobil.vertx.jsonpath.error

/**
 * A base interface for errors that can occur when executing a query that should yield exactly one
 * result.
 *
 * @see NoResult
 * @see MultipleResults
 */
sealed interface RequiredJsonValueError {
  /**
   * The query returned no result
   */
  data object NoResult : RequiredJsonValueError
}
