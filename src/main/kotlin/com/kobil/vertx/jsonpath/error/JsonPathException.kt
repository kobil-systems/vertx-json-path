package com.kobil.vertx.jsonpath.error

/**
 * A simple stacktrace-less exception carrying a [JsonPathError].
 *
 * @param err the [JsonPathError]
 */
class JsonPathException(
  val err: JsonPathError,
) : Exception(err.toString(), null, false, false)
