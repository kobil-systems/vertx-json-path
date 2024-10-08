package com.kobil.vertx.jsonpath;

import com.kobil.vertx.jsonpath.FilterExpression.Comparison;
import com.kobil.vertx.jsonpath.FilterExpression.Existence;
import com.kobil.vertx.jsonpath.FilterExpression.Not;
import com.kobil.vertx.jsonpath.QueryExpression.Relative;
import com.kobil.vertx.jsonpath.Segment.ChildSegment;
import kotlin.ranges.IntProgression;
import kotlin.ranges.IntRange;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static arrow.core.NonEmptyListKt.nonEmptyListOf;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class JavaSelectorTest {
    @Test
    @DisplayName("The name static function should return a Name selector with the given field name")
    public void name() {
        assertEquals(new Selector.Name("a"), Selector.name("a"));
        assertEquals(new Selector.Name("hello world"), Selector.name("hello world"));
    }

    @Test
    @DisplayName("The index static function should return an Index selector with the given index")
    public void index() {
        assertEquals(new Selector.Index(1), Selector.index(1));
        assertEquals(new Selector.Index(-10), Selector.index(-10));
    }

    @Nested
    @DisplayName("The slice static function")
    public class Slice {
        @Test
        @DisplayName("applied to an IntProgression instance should return an equivalent slice selector")
        public void progression() {
            assertEquals(new Selector.Slice(1, 3, 1), Selector.slice(new IntRange(1, 2)));
            assertEquals(new Selector.Slice(1, 5, 1), Selector.slice(new IntRange(1, 4)));
            assertEquals(new Selector.Slice(1, 10, 2), Selector.slice(new IntProgression(1, 10, 2)));
            assertEquals(new Selector.Slice(10, 0, -1), Selector.slice(new IntProgression(10, 1, -1)));
            assertEquals(new Selector.Slice(10, 1, -4), Selector.slice(new IntProgression(10, 1, -4)));
        }

        @Test
        @DisplayName("applied to two nullable integers should return an equivalent slice selector with unset step")
        public void twoInts() {
            assertEquals(new Selector.Slice(1, 2, null), Selector.slice(1, 2));
            assertEquals(new Selector.Slice(1, -1, null), Selector.slice(1, -1));
            assertEquals(new Selector.Slice(1, null, null), Selector.slice(1, null));
            assertEquals(new Selector.Slice(null, 2, null), Selector.slice(null, 2));
            assertEquals(new Selector.Slice(null, null, null), Selector.slice(null, null));
        }

        @Test
        @DisplayName("applied to three nullable integers should return an equivalent slice selector")
        public void threeInts() {
            assertEquals(new Selector.Slice(1, 2, 2), Selector.slice(1, 2, 2));
            assertEquals(new Selector.Slice(1, -1, 3), Selector.slice(1, -1, 3));
            assertEquals(new Selector.Slice(1, null, -1), Selector.slice(1, null, -1));
            assertEquals(new Selector.Slice(null, 2, 3), Selector.slice(null, 2, 3));
            assertEquals(new Selector.Slice(null, null, 3), Selector.slice(null, null, 3));
            assertEquals(new Selector.Slice(null, null, null), Selector.slice(null, null, null));
        }
    }

    @Test
    @DisplayName("The filter static function should return a Filter selector with the given filter expression")
    public void filter() {
        var expr1 = new Existence(new Relative(new ChildSegment("a")));
        var expr2 = new FilterExpression.Or(
                nonEmptyListOf(
                        new Comparison(
                                Comparison.Op.GREATER_EQ,
                                new Relative(new ChildSegment("a")),
                                new ComparableExpression.Literal(1)
                        ),
                        new Not(new Existence(new Relative(new ChildSegment("a"))))
                )
        );

        assertEquals(new Selector.Filter(expr1), Selector.filter(expr1));
        assertEquals(new Selector.Filter(expr2), Selector.filter(expr2));
    }
}
