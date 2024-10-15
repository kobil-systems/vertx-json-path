package com.kobil.vertx.jsonpath;

import arrow.core.Either;
import com.kobil.vertx.jsonpath.error.JsonPathError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class JavaJsonPathTest {
    @Test
    @DisplayName("A call to the compile static method with a valid JSON path should return a Right containing the compiled filter")
    public void success() {
        var jpA = assertInstanceOf(Either.Right.class, JsonPath.compile("$.a")).getValue();
        assertEquals(JsonPath.ROOT.field("a"), jpA);

        var jpB = assertInstanceOf(Either.Right.class, JsonPath.compile("$['b', 1::2, ?@.a][*]")).getValue();
        assertEquals(
                JsonPath.ROOT.selectChildren(
                        Selector.name("b"),
                        Selector.slice(1, null, 2),
                        Selector.filter(QueryExpression.relative().field("a").exists())
                ).selectAllChildren(),
                jpB
        );
    }

    @Test
    @DisplayName("A call to the compile static method with an invalid JSON Path string should return a Left")
    public void failure() {
        assertEquals(
                "QuestionMark",
                assertInstanceOf(
                        JsonPathError.UnexpectedToken.class,
                        assertInstanceOf(Either.Left.class, JsonPath.compile("?.abc")).getValue()
                ).getToken()
        );

        assertInstanceOf(
                JsonPathError.MustBeSingularQuery.class,
                assertInstanceOf(Either.Left.class, FilterExpression.compile("$[?@..a == 2]")).getValue()
        );
    }
}
