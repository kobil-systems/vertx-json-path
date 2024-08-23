package com.kobil.vertx.jsonpath.testing

import com.kobil.vertx.jsonpath.JsonPath
import io.kotest.property.Arb
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.of
import io.kotest.property.resolution.default

fun Codepoint.Companion.nameChar(): Arb<Codepoint> =
  Arb.of(
    ('0'.code..'9'.code).map(::Codepoint) +
      ('A'.code..'Z'.code).map(::Codepoint) +
      ('a'.code..'z'.code).map(::Codepoint) +
      Codepoint('_'.code) +
      (0x80..0xd7ff).map(::Codepoint) +
      (0xe000..0xffff).map(::Codepoint),
  )

fun Arb.Companion.normalizedJsonPath(): Arb<JsonPath> =
  arbitrary {
    val length = Arb.int(0..10).bind()
    var path = JsonPath.root

    for (i in 1..length) {
      path =
        if (Arb.boolean().bind()) {
          path[Arb.default<String>().bind()]
        } else {
          path[Arb.int(0..Int.MAX_VALUE).bind()]
        }
    }

    path
  }
