package com.kobil.vertx.jsonpath;

import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class JavaJsonNodeTest {
    @Test
    @DisplayName("The root static function should create a JsoNode with the given value and the root path")
    public void root() {
        Assertions.assertEquals(
                new JsonNode("a", JsonPath.ROOT),
                JsonNode.root("a")
        );

        Assertions.assertEquals(
                new JsonNode(1, JsonPath.ROOT),
                JsonNode.root(1)
        );

        Assertions.assertEquals(
                new JsonNode(null, JsonPath.ROOT),
                JsonNode.root(null)
        );

        Assertions.assertEquals(
                new JsonNode(new JsonObject(), JsonPath.ROOT),
                JsonNode.root(new JsonObject())
        );
    }
}
