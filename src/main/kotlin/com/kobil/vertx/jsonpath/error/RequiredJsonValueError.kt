package com.kobil.vertx.jsonpath.error

sealed interface RequiredJsonValueError {
  data object NoResult : RequiredJsonValueError
}
