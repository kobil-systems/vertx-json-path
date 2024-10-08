package com.kobil.vertx.jsonpath

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import com.kobil.vertx.jsonpath.JsonNode.Companion.rootNode
import com.kobil.vertx.jsonpath.compiler.JsonPathCompiler
import com.kobil.vertx.jsonpath.error.JsonPathError
import com.kobil.vertx.jsonpath.interpreter.test
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

sealed interface FilterExpression {
  fun test(obj: JsonObject): Boolean = test(obj.rootNode)

  fun test(arr: JsonArray): Boolean = test(arr.rootNode)

  infix fun and(other: FilterExpression): FilterExpression =
    if (this is And && other is And) {
      And(operands + other.operands)
    } else if (this is And) {
      And(operands + other)
    } else if (other is And) {
      And(NonEmptyList(this, other.operands.toList()))
    } else {
      And(nonEmptyListOf(this, other))
    }

  infix fun or(other: FilterExpression): FilterExpression =
    if (this is Or && other is Or) {
      Or(operands + other.operands)
    } else if (this is Or) {
      Or(operands + other)
    } else if (other is Or) {
      Or(NonEmptyList(this, other.operands.toList()))
    } else {
      Or(nonEmptyListOf(this, other))
    }

  operator fun not(): FilterExpression =
    when (this) {
      is Not -> operand
      is Comparison -> copy(op = op.inverse)
      else -> Not(this)
    }

  data class And(
    val operands: NonEmptyList<FilterExpression>,
  ) : FilterExpression {
    constructor(firstOperand: FilterExpression, vararg moreOperands: FilterExpression) : this(
      nonEmptyListOf(firstOperand, *moreOperands),
    )

    override fun toString(): String =
      operands.joinToString(" && ") {
        if (it is Or) {
          "($it)"
        } else {
          "$it"
        }
      }
  }

  data class Or(
    val operands: NonEmptyList<FilterExpression>,
  ) : FilterExpression {
    constructor(firstOperand: FilterExpression, vararg moreOperands: FilterExpression) : this(
      nonEmptyListOf(firstOperand, *moreOperands),
    )

    override fun toString(): String = operands.joinToString(" || ")
  }

  data class Not(
    val operand: FilterExpression,
  ) : FilterExpression {
    override fun toString(): String =
      if (operand is Comparison || operand is And || operand is Or) {
        "!($operand)"
      } else {
        "!$operand"
      }
  }

  data class Comparison(
    val op: Op,
    val lhs: ComparableExpression,
    val rhs: ComparableExpression,
  ) : FilterExpression {
    enum class Op(
      val str: String,
    ) {
      EQ("=="),
      NOT_EQ("!="),
      LESS("<"),
      LESS_EQ("<="),
      GREATER(">"),
      GREATER_EQ(">="),
      ;

      val inverse: Op
        get() =
          when (this) {
            EQ -> NOT_EQ
            NOT_EQ -> EQ
            LESS -> GREATER_EQ
            LESS_EQ -> GREATER
            GREATER -> LESS_EQ
            GREATER_EQ -> LESS
          }
    }

    override fun toString(): String = "$lhs ${op.str} $rhs"

    companion object {
      fun eq(
        lhs: ComparableExpression,
        rhs: ComparableExpression,
      ): Comparison = Comparison(Op.EQ, lhs, rhs)

      fun neq(
        lhs: ComparableExpression,
        rhs: ComparableExpression,
      ): Comparison = Comparison(Op.NOT_EQ, lhs, rhs)

      fun greaterThan(
        lhs: ComparableExpression,
        rhs: ComparableExpression,
      ): Comparison = Comparison(Op.GREATER, lhs, rhs)

      fun greaterOrEqual(
        lhs: ComparableExpression,
        rhs: ComparableExpression,
      ): Comparison = Comparison(Op.GREATER_EQ, lhs, rhs)

      fun lessThan(
        lhs: ComparableExpression,
        rhs: ComparableExpression,
      ): Comparison = Comparison(Op.LESS, lhs, rhs)

      fun lessOrEqual(
        lhs: ComparableExpression,
        rhs: ComparableExpression,
      ): Comparison = Comparison(Op.LESS_EQ, lhs, rhs)
    }
  }

  data class Existence(
    val query: NodeListExpression,
  ) : FilterExpression {
    override fun toString(): String = "$query"
  }

  data class Match(
    val subject: ComparableExpression,
    val pattern: ComparableExpression,
    val matchEntire: Boolean,
  ) : FilterExpression {
    override fun toString(): String {
      val name = if (matchEntire) "match" else "search"

      return "$name($subject, $pattern)"
    }
  }

  companion object {
    @JvmStatic
    fun compile(filterExpression: String): Either<JsonPathError, FilterExpression> =
      JsonPathCompiler.compileJsonPathFilter(filterExpression)
  }
}
