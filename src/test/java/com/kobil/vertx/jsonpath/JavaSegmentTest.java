package com.kobil.vertx.jsonpath;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class JavaSegmentTest {
    @Nested
    @DisplayName("The child static method on Segment")
    public class Child {
        @Test
        @DisplayName("should throw when called with an empty list")
        public void emptyList() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> Segment.child(new ArrayList<>())
            );
        }

        @Test
        @DisplayName("should return a child segment with the same selectors when called with a non-empty list")
        public void nonEmptyList() {
            var sel = new ArrayList<Selector>();
            sel.add(Selector.name("a"));
            sel.add(Selector.index(1));
            sel.add(Selector.slice(1, -1, 2));
            sel.add(Selector.WILDCARD);
            sel.add(Selector.filter(QueryExpression.absolute().exists()));

            assertEquals(
                    new Segment.ChildSegment(sel),
                    Segment.child(sel)
            );
        }

        @Test
        @DisplayName("should return a child segment with the same selectors when called with varargs")
        public void varargs() {
            var n = Selector.name("a");
            var i = Selector.index(1);
            var s = Selector.slice(1, -1, 2);
            var w = Selector.WILDCARD;
            var f = Selector.filter(QueryExpression.absolute().exists());

            assertEquals(
                    new Segment.ChildSegment(Arrays.asList(n, i, s, w, f)),
                    Segment.child(n, i, s, w, f)
            );
        }
    }

    @Nested
    @DisplayName("The descendant static method on Segment")
    public class Descendant {
        @Test
        @DisplayName("should throw when called with an empty list")
        public void emptyList() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> Segment.descendant(new ArrayList<>())
            );
        }

        @Test
        @DisplayName("should return a descendant segment with the same selectors when called with a non-empty list")
        public void nonEmptyList() {
            var sel = new ArrayList<Selector>();
            sel.add(Selector.name("a"));
            sel.add(Selector.index(1));
            sel.add(Selector.slice(1, -1, 2));
            sel.add(Selector.WILDCARD);
            sel.add(Selector.filter(QueryExpression.absolute().exists()));

            assertEquals(
                    new Segment.DescendantSegment(sel),
                    Segment.descendant(sel)
            );
        }

        @Test
        @DisplayName("should return a descendant segment with the same selectors when called with varargs")
        public void varargs() {
            var n = Selector.name("a");
            var i = Selector.index(1);
            var s = Selector.slice(1, -1, 2);
            var w = Selector.WILDCARD;
            var f = Selector.filter(QueryExpression.absolute().exists());

            assertEquals(
                    new Segment.DescendantSegment(Arrays.asList(n, i, s, w, f)),
                    Segment.descendant(n, i, s, w, f)
            );
        }
    }
}
