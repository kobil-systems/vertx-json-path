package com.kobil.vertx.jsonpath.testing

import io.kotest.core.spec.style.scopes.ShouldSpecContainerScope

typealias SpecContext<Container> = suspend Container.(String, suspend Container.() -> Unit) -> Unit
typealias ShouldSpecContext = SpecContext<ShouldSpecContainerScope>
