package com.kobil.vertx.jsonpath;

import arrow.core.Either;
import com.kobil.vertx.jsonpath.compiler.Token;
import com.kobil.vertx.jsonpath.error.JsonPathError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class JavaFilterExpressionTest {
    @Test
    @DisplayName("A call to the compile static method with a valid filter expression should return a Right containing the compiled filter")
    public void success() {
        var filterA = assertInstanceOf(Either.Right.class, FilterExpression.compile("@.a")).getValue();
        assertEquals(new FilterExpression.Existence(QueryExpression.relative().field("a")), filterA);

        var filterB = assertInstanceOf(Either.Right.class, FilterExpression.compile("$['b']")).getValue();
        assertEquals(new FilterExpression.Existence(QueryExpression.absolute().field("b")), filterB);
    }

    @Test
    @DisplayName("A call to the compile static method with an invalid filter string should return a Left")
    public void failure() {
        assertInstanceOf(
                Token.QuestionMark.class,
                assertInstanceOf(
                        JsonPathError.UnexpectedToken.class,
                        assertInstanceOf(Either.Left.class, FilterExpression.compile("?@.abc")).getValue()
                ).getToken()
        );

        assertInstanceOf(
                JsonPathError.MustBeSingularQuery.class,
                assertInstanceOf(Either.Left.class, FilterExpression.compile("@..a == 2")).getValue()
        );
    }
}
