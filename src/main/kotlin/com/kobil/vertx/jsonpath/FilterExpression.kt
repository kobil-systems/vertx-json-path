package com.kobil.vertx.jsonpath

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import com.kobil.vertx.jsonpath.FilterExpression.Comparison.Op
import com.kobil.vertx.jsonpath.JsonNode.Companion.rootNode
import com.kobil.vertx.jsonpath.compiler.JsonPathCompiler
import com.kobil.vertx.jsonpath.error.JsonPathError
import com.kobil.vertx.jsonpath.interpreter.test
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

/**
 * A base type for filter expressions which may be used in a filter selector,
 * or on its own as a predicate.
 */
sealed class FilterExpression {
  /**
   * Tests whether this filter expression matches the given JSON object.
   *
   * @param obj the JSON object to match
   * @return true, if the filter matches the object, false, otherwise
   */
  fun test(obj: JsonObject): Boolean = test(obj.rootNode)

  /**
   * Tests whether this filter expression matches the given JSON array.
   *
   * @param arr the JSON array to match
   * @return true, if the filter matches the array, false, otherwise
   */
  fun test(arr: JsonArray): Boolean = test(arr.rootNode)

  /**
   * Constructs a filter expression testing that both, this expression and [other], are satisfied.
   * This is equivalent to the '&&' operator in JSON Path.
   *
   * @param other the right hand operand of the '&&' operator
   * @return the '&&' expression testing both operands
   */
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

  /**
   * Constructs a filter expression testing that at least one of this expression and [other],
   * is satisfied. This is equivalent to the '||' operator in JSON Path.
   *
   * @param other the right hand operand of the '||' operator
   * @return the '||' expression testing that at least one operand is satisfied
   */
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

  /**
   * Inverts this filter expression. This is equivalent to the '!' operator in JSON Path.
   *
   * @return the inverted filter expression
   */
  operator fun not(): FilterExpression =
    when (this) {
      is Not -> operand
      is Comparison -> copy(op = op.inverse)
      else -> Not(this)
    }

  /**
   * A logical AND connection of the operands
   *
   * @param operands a non-empty list of operands
   */
  data class And(
    val operands: NonEmptyList<FilterExpression>,
  ) : FilterExpression() {
    /**
     * An alternative constructor, taking the operands as varargs.
     *
     * @param firstOperand the first operand of '&&'
     * @param secondOperand the second operand of '&&'
     * @param moreOperands all remaining operands of '&&', if any
     */
    constructor(
      firstOperand: FilterExpression,
      secondOperand: FilterExpression,
      vararg moreOperands: FilterExpression,
    ) : this(
      nonEmptyListOf(firstOperand, secondOperand, *moreOperands),
    )

    /**
     * Returns the JSON path representation of this And expression.
     */
    override fun toString(): String =
      operands.joinToString(" && ") {
        if (it is Or) {
          "($it)"
        } else {
          "$it"
        }
      }
  }

  /**
   * A logical Or connection of the operands
   *
   * @param operands a non-empty list of operands
   */
  data class Or(
    val operands: NonEmptyList<FilterExpression>,
  ) : FilterExpression() {
    /**
     * An alternative constructor, taking the operands as varargs.
     *
     * @param firstOperand the first operand of '&&'
     * @param secondOperand the second operand of '&&'
     * @param moreOperands all remaining operands of '&&', if any
     */
    constructor(
      firstOperand: FilterExpression,
      secondOperand: FilterExpression,
      vararg moreOperands: FilterExpression,
    ) : this(
      nonEmptyListOf(firstOperand, secondOperand, *moreOperands),
    )

    /**
     * Returns the JSON path representation of this Or expression.
     */
    override fun toString(): String = operands.joinToString(" || ")
  }

  /**
   * The logical inversion of the operand.
   *
   * @param operand the operand that is inverted
   */
  data class Not(
    val operand: FilterExpression,
  ) : FilterExpression() {
    /**
     * Returns the JSON path representation of this Not expression.
     */
    override fun toString(): String =
      if (operand is Comparison || operand is And || operand is Or) {
        "!($operand)"
      } else {
        "!$operand"
      }
  }

  /**
   * A comparison expression.
   *
   * @param op the comparison operator, see [Op]
   * @param lhs the left hand operand of the comparison
   * @param rhs the right hand operand of the comparison
   */
  data class Comparison(
    val op: Op,
    val lhs: ComparableExpression,
    val rhs: ComparableExpression,
  ) : FilterExpression() {
    /**
     * A comparison operator.
     *
     * @param str the string representation of the operator
     */
    enum class Op(
      val str: String,
    ) {
      /**
       * The '==' operator
       */
      EQ("=="),

      /**
       * The '"="=' operator
       */
      NOT_EQ("!="),

      /**
       * The '<' operator
       */
      LESS("<"),

      /**
       * The '<=' operator
       */
      LESS_EQ("<="),

      /**
       * The '>' operator
       */
      GREATER(">"),

      /**
       * The '>=' operator
       */
      GREATER_EQ(">="),
      ;

      /**
       * The logical inverse of the operator
       */
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

    /**
     * Returns the JSON path representation of this comparison expression.
     */
    override fun toString(): String = "$lhs ${op.str} $rhs"

    /**
     * Provides useful helper functions to construct comparison expressions.
     */
    companion object {
      /**
       * Construct a [FilterExpression.Comparison] checking that [lhs] is equal to [rhs]
       *
       * @param lhs the left hand side of the comparison
       * @param rhs the right hand side of the comparison
       * @return a '==' comparison
       */
      fun eq(
        lhs: ComparableExpression,
        rhs: ComparableExpression,
      ): Comparison = Comparison(Op.EQ, lhs, rhs)

      /**
       * Construct a [FilterExpression.Comparison] checking that [lhs] is not equal to [rhs]
       *
       * @param lhs the left hand side of the comparison
       * @param rhs the right hand side of the comparison
       * @return a '!=' comparison
       */
      fun neq(
        lhs: ComparableExpression,
        rhs: ComparableExpression,
      ): Comparison = Comparison(Op.NOT_EQ, lhs, rhs)

      /**
       * Construct a [FilterExpression.Comparison] checking that [lhs] is greater than [rhs]
       *
       * @param lhs the left hand side of the comparison
       * @param rhs the right hand side of the comparison
       * @return a '>' comparison
       */
      fun greaterThan(
        lhs: ComparableExpression,
        rhs: ComparableExpression,
      ): Comparison = Comparison(Op.GREATER, lhs, rhs)

      /**
       * Construct a [FilterExpression.Comparison] checking that [lhs] is greater than
       * or equal to [rhs]
       *
       * @param lhs the left hand side of the comparison
       * @param rhs the right hand side of the comparison
       * @return a '>=' comparison
       */
      fun greaterOrEqual(
        lhs: ComparableExpression,
        rhs: ComparableExpression,
      ): Comparison = Comparison(Op.GREATER_EQ, lhs, rhs)

      /**
       * Construct a [FilterExpression.Comparison] checking that [lhs] is less than [rhs]
       *
       * @param lhs the left hand side of the comparison
       * @param rhs the right hand side of the comparison
       * @return a '<' comparison
       */
      fun lessThan(
        lhs: ComparableExpression,
        rhs: ComparableExpression,
      ): Comparison = Comparison(Op.LESS, lhs, rhs)

      /**
       * Construct a [FilterExpression.Comparison] checking that [lhs] is less than
       * or equal to [rhs]
       *
       * @param lhs the left hand side of the comparison
       * @param rhs the right hand side of the comparison
       * @return a '<=' comparison
       */
      fun lessOrEqual(
        lhs: ComparableExpression,
        rhs: ComparableExpression,
      ): Comparison = Comparison(Op.LESS_EQ, lhs, rhs)
    }
  }

  /**
   * A filter expression checking for the existence of any elements satisfying the query (or,
   * generally, the node list expression).
   *
   * @param query the node list expression, most likely a [QueryExpression]
   * @see [QueryExpression]
   */
  data class Test(
    val query: NodeListExpression,
  ) : FilterExpression() {
    /**
     * Returns the JSON path representation of this Test expression, i.e. the serialized query
     */
    override fun toString(): String = "$query"
  }

  /**
   * A filter expression that matches some string [subject] against some regex [pattern]. It may
   * either require the entire subject or a substring of the subject to match the pattern.
   *
   * @param subject the subject, which must be an expression referring to a string
   *   (e.g. a [ComparableExpression.Literal] or a [QueryExpression] pointing to a string value)
   * @param pattern the regex pattern, which must be an expression referring to a string
   *   (e.g. a [ComparableExpression.Literal] or a [QueryExpression] pointing to a string value)
   * @param matchEntire whether the entire subject string should be matched. If false, the function
   *   looks for a matching substring.
   */
  data class Match(
    val subject: ComparableExpression,
    val pattern: ComparableExpression,
    val matchEntire: Boolean,
  ) : FilterExpression() {
    /**
     * Returns the JSON path representation of this function expression.
     */
    override fun toString(): String {
      val name = if (matchEntire) "match" else "search"

      return "$name($subject, $pattern)"
    }
  }

  /**
   * Contains static functions to construct filter expressions
   */
  companion object {
    /**
     * Compiles a filter expression string. It will return an instance of [Either.Right] containing
     * the compiled expression if the compilation was successful. Otherwise, an error wrapped in an
     * instance of [Either.Left] is returned.
     *
     * Filter expression strings must not have a leading `?`.
     *
     * @param filterExpression the filter expression string without leading `?`
     * @return the compiled filter expression, or an error
     * @see JsonPathError
     */
    @JvmStatic
    fun compile(filterExpression: String): Either<JsonPathError, FilterExpression> =
      JsonPathCompiler.compileJsonPathFilter(filterExpression)
  }
}
