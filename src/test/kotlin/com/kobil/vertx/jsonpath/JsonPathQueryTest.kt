package com.kobil.vertx.jsonpath

import com.kobil.vertx.jsonpath.FilterExpression.Test
import com.kobil.vertx.jsonpath.QueryExpression.Relative
import com.kobil.vertx.jsonpath.Segment.ChildSegment
import com.kobil.vertx.jsonpath.Segment.DescendantSegment
import com.kobil.vertx.jsonpath.Selector.Index
import com.kobil.vertx.jsonpath.Selector.Name
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.orNull
import io.kotest.property.arbitrary.string
import io.kotest.property.arbitrary.triple
import io.kotest.property.checkAll

class JsonPathQueryTest :
  ShouldSpec({
    context("The get operator") {
      context("applied to one or more strings") {
        should("append a child segment selecting the specified field names") {
          checkAll(Arb.string(), Arb.string(), Arb.string()) { a, b, c ->
            QueryExpression.absolute()[a].segments shouldBe
              listOf(
                ChildSegment(Name(a)),
              )

            QueryExpression.absolute()[a, b].segments shouldBe
              listOf(
                ChildSegment(listOf(Name(a), Name(b))),
              )

            QueryExpression.absolute()[a, c].segments shouldBe
              listOf(
                ChildSegment(listOf(Name(a), Name(c))),
              )

            QueryExpression.absolute()[a, b, c].segments shouldBe
              listOf(
                ChildSegment(listOf(Name(a), Name(b), Name(c))),
              )

            QueryExpression.absolute()[a, c][b].segments shouldBe
              listOf(
                ChildSegment(listOf(Name(a), Name(c))),
                ChildSegment(listOf(Name(b))),
              )
          }
        }
      }

      context("applied to one or more integers") {
        should("append a child segment selecting the specified indices") {
          checkAll(Arb.int(), Arb.int(), Arb.int()) { a, b, c ->
            QueryExpression.absolute()[a].segments shouldBe
              listOf(
                ChildSegment(Index(a)),
              )

            QueryExpression.absolute()[a, b].segments shouldBe
              listOf(
                ChildSegment(listOf(Index(a), Index(b))),
              )

            QueryExpression.absolute()[a, c].segments shouldBe
              listOf(
                ChildSegment(listOf(Index(a), Index(c))),
              )

            QueryExpression.absolute()[a, b, c].segments shouldBe
              listOf(
                ChildSegment(listOf(Index(a), Index(b), Index(c))),
              )

            QueryExpression.absolute()[a, c][b].segments shouldBe
              listOf(
                ChildSegment(listOf(Index(a), Index(c))),
                ChildSegment(listOf(Index(b))),
              )
          }
        }
      }

      context("applied to one or more selectors") {
        should("append a child segment using the specified selectors") {
          checkAll(
            Arb.string(),
            Arb.int(),
            Arb.triple(Arb.int().orNull(), Arb.int().orNull(), Arb.int().orNull()),
          ) { name, idx, (first, last, step) ->
            val n = Selector.name(name)
            val i = Selector.index(idx)
            val w = Selector.Wildcard
            val s = Selector.slice(first, last, step)
            val f = Selector.filter(Test(Relative()["a"]))

            QueryExpression.absolute()[n].segments shouldBe
              listOf(
                ChildSegment(n),
              )

            QueryExpression.absolute()[n, i].segments shouldBe
              listOf(
                ChildSegment(listOf(n, i)),
              )

            QueryExpression.absolute()[n, i, s].segments shouldBe
              listOf(
                ChildSegment(listOf(n, i, s)),
              )

            QueryExpression.absolute()[n, i][s, w].segments shouldBe
              listOf(
                ChildSegment(listOf(n, i)),
                ChildSegment(listOf(s, w)),
              )

            QueryExpression.absolute()[n, i, f][w][i, f, s].segments shouldBe
              listOf(
                ChildSegment(listOf(n, i, f)),
                ChildSegment(listOf(w)),
                ChildSegment(listOf(i, f, s)),
              )
          }
        }
      }
    }

    context("The selectChildren function") {
      should("append a child segment using the specified selectors") {
        checkAll(
          Arb.string(),
          Arb.int(),
          Arb.triple(Arb.int().orNull(), Arb.int().orNull(), Arb.int().orNull()),
        ) { name, idx, (first, last, step) ->
          val n = Selector.name(name)
          val i = Selector.index(idx)
          val w = Selector.Wildcard
          val s = Selector.slice(first, last, step)
          val f = Selector.filter(Test(Relative()["a"]))

          QueryExpression.absolute().selectChildren(n).segments shouldBe
            listOf(
              ChildSegment(n),
            )

          QueryExpression.absolute().selectChildren(n, i).segments shouldBe
            listOf(
              ChildSegment(listOf(n, i)),
            )

          QueryExpression.absolute().selectChildren(n, i, s).segments shouldBe
            listOf(
              ChildSegment(listOf(n, i, s)),
            )

          QueryExpression
            .absolute()
            .selectChildren(n, i)
            .selectChildren(s, w)
            .segments shouldBe
            listOf(
              ChildSegment(listOf(n, i)),
              ChildSegment(listOf(s, w)),
            )

          QueryExpression
            .absolute()
            .selectChildren(
              n,
              i,
              f,
            ).selectChildren(w)
            .selectChildren(i, f, s)
            .segments shouldBe
            listOf(
              ChildSegment(listOf(n, i, f)),
              ChildSegment(listOf(w)),
              ChildSegment(listOf(i, f, s)),
            )
        }
      }
    }

    context("The selectDescendants function") {
      should("append a descendant segment using the specified selectors") {
        checkAll(
          Arb.string(),
          Arb.int(),
          Arb.triple(Arb.int().orNull(), Arb.int().orNull(), Arb.int().orNull()),
        ) { name, idx, (first, last, step) ->
          val n = Selector.name(name)
          val i = Selector.index(idx)
          val w = Selector.Wildcard
          val s = Selector.slice(first, last, step)
          val f = Selector.filter(Test(Relative()["a"]))

          QueryExpression.absolute().selectDescendants(n).segments shouldBe
            listOf(
              DescendantSegment(n),
            )

          QueryExpression.absolute().selectDescendants(n, i).segments shouldBe
            listOf(
              DescendantSegment(listOf(n, i)),
            )

          QueryExpression.absolute().selectDescendants(n, i, s).segments shouldBe
            listOf(
              DescendantSegment(listOf(n, i, s)),
            )

          QueryExpression
            .absolute()
            .selectChildren(n, i)
            .selectDescendants(s, w)
            .segments shouldBe
            listOf(
              ChildSegment(listOf(n, i)),
              DescendantSegment(listOf(s, w)),
            )

          QueryExpression
            .absolute()
            .selectDescendants(
              n,
              i,
              f,
            ).selectChildren(w)
            .selectDescendants(i, f, s)
            .segments shouldBe
            listOf(
              DescendantSegment(listOf(n, i, f)),
              ChildSegment(listOf(w)),
              DescendantSegment(listOf(i, f, s)),
            )
        }
      }
    }

    context("The selectAllChildren function") {
      should("append a wildcard selector child segment") {
        QueryExpression.absolute().selectAllChildren().segments shouldBe
          listOf(
            ChildSegment(Selector.WILDCARD),
          )

        QueryExpression
          .absolute()
          .field("a")
          .selectAllChildren()
          .segments shouldBe
          listOf(
            ChildSegment(Name("a")),
            ChildSegment(Selector.Wildcard),
          )
      }
    }

    context("The selectAllDescendants function") {
      should("append a wildcard selector descendant segment") {
        QueryExpression.absolute().selectAllDescendants().segments shouldBe
          listOf(
            DescendantSegment(Selector.WILDCARD),
          )

        QueryExpression
          .absolute()
          .field("a")
          .selectAllDescendants()
          .segments shouldBe
          listOf(
            ChildSegment(Name("a")),
            DescendantSegment(Selector.Wildcard),
          )
      }
    }

    context("The field function") {
      should("append a child segment selecting the specified field names") {
        checkAll(Arb.string(), Arb.string(), Arb.string()) { a, b, c ->
          QueryExpression.absolute().field(a).segments shouldBe
            listOf(
              ChildSegment(Name(a)),
            )

          QueryExpression.absolute().field(a, b).segments shouldBe
            listOf(
              ChildSegment(listOf(Name(a), Name(b))),
            )

          QueryExpression.absolute().field(a, c).segments shouldBe
            listOf(
              ChildSegment(listOf(Name(a), Name(c))),
            )

          QueryExpression.absolute().field(a, b, c).segments shouldBe
            listOf(
              ChildSegment(listOf(Name(a), Name(b), Name(c))),
            )

          QueryExpression
            .absolute()
            .field(a, c)
            .field(b)
            .segments shouldBe
            listOf(
              ChildSegment(listOf(Name(a), Name(c))),
              ChildSegment(listOf(Name(b))),
            )
        }
      }
    }

    context("The index function") {
      should("append a child segment selecting the specified indices") {
        checkAll(Arb.int(), Arb.int(), Arb.int()) { a, b, c ->
          QueryExpression.absolute().index(a).segments shouldBe
            listOf(
              ChildSegment(Index(a)),
            )

          QueryExpression.absolute().index(a, b).segments shouldBe
            listOf(
              ChildSegment(listOf(Index(a), Index(b))),
            )

          QueryExpression.absolute().index(a, c).segments shouldBe
            listOf(
              ChildSegment(listOf(Index(a), Index(c))),
            )

          QueryExpression.absolute().index(a, b, c).segments shouldBe
            listOf(
              ChildSegment(listOf(Index(a), Index(b), Index(c))),
            )

          QueryExpression
            .absolute()
            .index(a, c)
            .index(b)
            .segments shouldBe
            listOf(
              ChildSegment(listOf(Index(a), Index(c))),
              ChildSegment(listOf(Index(b))),
            )
        }
      }
    }
  })
