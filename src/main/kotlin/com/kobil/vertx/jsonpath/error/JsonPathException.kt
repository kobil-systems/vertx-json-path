package com.kobil.vertx.jsonpath.error

class JsonPathException(
  val err: JsonPathError,
) : Exception(err.toString(), (err as? JsonPathError.UnexpectedError)?.cause, false, false)
