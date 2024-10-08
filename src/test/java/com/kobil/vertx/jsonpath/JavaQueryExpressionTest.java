package com.kobil.vertx.jsonpath;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JavaQueryExpressionTest {
    @Nested
    @DisplayName("The absolute static function")
    public class Absolute {
        @Test
        @DisplayName("called without arguments should return an empty absolute query")
        public void noArgs() {
            assertEquals(
                    new QueryExpression.Absolute(Collections.emptyList()),
                    QueryExpression.absolute()
            );
        }

        @Test
        @DisplayName("called with a list of segments should return an absolute query using the same segments")
        public void list() {
            var n = Selector.name("a");
            var i = Selector.index(1);
            var s = Selector.slice(1, -1, 2);
            var w = Selector.WILDCARD;
            var f = Selector.filter(QueryExpression.absolute().exists());

            var segments1 = List.of(new Segment.ChildSegment(n));
            var segments2 = Arrays.asList(new Segment.ChildSegment(n, i), new Segment.DescendantSegment(f));
            var segments3 = Arrays.asList(new Segment.ChildSegment(w), new Segment.DescendantSegment(s),
                    new Segment.ChildSegment(i));

            assertEquals(
                    new QueryExpression.Absolute(segments1),
                    QueryExpression.absolute(segments1)
            );

            assertEquals(
                    new QueryExpression.Absolute(segments2),
                    QueryExpression.absolute(segments2)
            );

            assertEquals(
                    new QueryExpression.Absolute(segments3),
                    QueryExpression.absolute(segments3)
            );
        }

        @Test
        @DisplayName("called with vararg segments should return an absolute query using the same segments")
        public void varargs() {
            var n = Selector.name("a");
            var i = Selector.index(1);
            var s = Selector.slice(1, -1, 2);
            var w = Selector.WILDCARD;
            var f = Selector.filter(QueryExpression.absolute().exists());

            var segments1 = List.of(new Segment.ChildSegment(n));
            var segments2 = Arrays.asList(new Segment.ChildSegment(n, i), new Segment.DescendantSegment(f));
            var segments3 = Arrays.asList(new Segment.ChildSegment(w), new Segment.DescendantSegment(s),
                    new Segment.ChildSegment(i));

            assertEquals(
                    new QueryExpression.Absolute(segments1),
                    QueryExpression.absolute(new Segment.ChildSegment(n))
            );

            assertEquals(
                    new QueryExpression.Absolute(segments2),
                    QueryExpression.absolute(new Segment.ChildSegment(n, i), new Segment.DescendantSegment(f))
            );

            assertEquals(
                    new QueryExpression.Absolute(segments3),
                    QueryExpression.absolute(new Segment.ChildSegment(w), new Segment.DescendantSegment(s),
                            new Segment.ChildSegment(i))
            );
        }
    }

    @Nested
    @DisplayName("The relative static function")
    public class Relative {
        @Test
        @DisplayName("called without arguments should return an empty relative query")
        public void noArgs() {
            assertEquals(
                    new QueryExpression.Relative(Collections.emptyList()),
                    QueryExpression.relative()
            );
        }

        @Test
        @DisplayName("called with a list of segments should return a relative query using the same segments")
        public void list() {
            var n = Selector.name("a");
            var i = Selector.index(1);
            var s = Selector.slice(1, -1, 2);
            var w = Selector.WILDCARD;
            var f = Selector.filter(QueryExpression.relative().exists());

            var segments1 = List.of(new Segment.ChildSegment(n));
            var segments2 = Arrays.asList(new Segment.ChildSegment(n, i), new Segment.DescendantSegment(f));
            var segments3 = Arrays.asList(new Segment.ChildSegment(w), new Segment.DescendantSegment(s),
                    new Segment.ChildSegment(i));

            assertEquals(
                    new QueryExpression.Relative(segments1),
                    QueryExpression.relative(segments1)
            );

            assertEquals(
                    new QueryExpression.Relative(segments2),
                    QueryExpression.relative(segments2)
            );

            assertEquals(
                    new QueryExpression.Relative(segments3),
                    QueryExpression.relative(segments3)
            );
        }

        @Test
        @DisplayName("called with vararg segments should return a relative query using the same segments")
        public void varargs() {
            var n = Selector.name("a");
            var i = Selector.index(1);
            var s = Selector.slice(1, -1, 2);
            var w = Selector.WILDCARD;
            var f = Selector.filter(QueryExpression.relative().exists());

            var segments1 = List.of(new Segment.ChildSegment(n));
            var segments2 = Arrays.asList(new Segment.ChildSegment(n, i), new Segment.DescendantSegment(f));
            var segments3 = Arrays.asList(new Segment.ChildSegment(w), new Segment.DescendantSegment(s),
                    new Segment.ChildSegment(i));

            assertEquals(
                    new QueryExpression.Relative(segments1),
                    QueryExpression.relative(new Segment.ChildSegment(n))
            );

            assertEquals(
                    new QueryExpression.Relative(segments2),
                    QueryExpression.relative(new Segment.ChildSegment(n, i), new Segment.DescendantSegment(f))
            );

            assertEquals(
                    new QueryExpression.Relative(segments3),
                    QueryExpression.relative(new Segment.ChildSegment(w), new Segment.DescendantSegment(s),
                            new Segment.ChildSegment(i))
            );
        }
    }
}
