package com.kobil.vertx.jsonpath;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JavaComparableExpressionTest {
    @Test
    @DisplayName("The literalNull static function should return a Literal with null value")
    public void literalNull() {
        assertEquals(
                new ComparableExpression.Literal(null),
                ComparableExpression.literalNull()
        );
    }

    @Nested
    @DisplayName("The literal static function")
    public class Literal {
        @Test
        @DisplayName("applied to an integer should return a Literal with the same value")
        public void integer() {
            assertEquals(
                    new ComparableExpression.Literal(1),
                    ComparableExpression.literal(1)
            );

            assertEquals(
                    new ComparableExpression.Literal(123),
                    ComparableExpression.literal(123)
            );
        }

        @Test
        @DisplayName("applied to a floating point number should return a Literal with the same value")
        public void floatingPoint() {
            assertEquals(
                    new ComparableExpression.Literal(1.5),
                    ComparableExpression.literal(1.5)
            );

            assertEquals(
                    new ComparableExpression.Literal(0.123),
                    ComparableExpression.literal(0.123)
            );
        }

        @Test
        @DisplayName("applied to a string should return a Literal with the same value")
        public void string() {
            assertEquals(
                    new ComparableExpression.Literal("a"),
                    ComparableExpression.literal("a")
            );

            assertEquals(
                    new ComparableExpression.Literal("hello world"),
                    ComparableExpression.literal("hello world")
            );
        }

        @Test
        @DisplayName("applied to a boolean should return a Literal with the same value")
        public void bool() {
            assertEquals(
                    new ComparableExpression.Literal(true),
                    ComparableExpression.literal(true)
            );

            assertEquals(
                    new ComparableExpression.Literal(false),
                    ComparableExpression.literal(false)
            );
        }
    }

}
