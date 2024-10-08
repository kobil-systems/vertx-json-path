package com.kobil.vertx.jsonpath;

import arrow.core.Either;
import com.kobil.vertx.jsonpath.error.JsonPathError;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class Compile {
    private static JsonPath unwrap(Either<JsonPathError, JsonPath> result) {
        return result.fold(
                err -> {
                    throw new IllegalStateException(err.toString());
                },
                r -> r
        );
    }

    public static void main(String[] args) {
        var jp = "$.a[1].b[1:3,4:-1:2]";

        var compiled = unwrap(JsonPath.compile(jp));

        System.out.println(compiled);
        System.out.println(compiled.field("c"));

        var json = new JsonObject();
        var a = new JsonArray();
        var second = new JsonObject();
        var b = new JsonArray();

        json.put("a", a);

        a.add(1);
        a.add(second);

        second.put("b", b);

        for (int i = 0; i < 12; ++i) {
            b.add(i);
        }

        System.out.println(compiled.evaluate(json));

        System.out.println(compiled.getOne(json));
        System.out.println(compiled.requireOne(json));
        System.out.println(compiled.getAll(json));
        System.out.println(compiled.traceOne(json));
        System.out.println(compiled.traceAll(json));

        var jp2 = unwrap(JsonPath.compile("$.a"));
        System.out.println(jp2.getOne(json));
        System.out.println(jp2.getOne(a));
        System.out.println(jp2.requireOne(json));
        System.out.println(jp2.requireOne(a));
        System.out.println(jp2.getAll(json));
        System.out.println(jp2.getAll(a));
        System.out.println(jp2.traceOne(json));
        System.out.println(jp2.traceOne(a));
        System.out.println(jp2.traceAll(json));
        System.out.println(jp2.traceAll(a));

        var expr = "@.a";
        var filter = FilterExpression.compile(expr).fold(
                err -> {
                    throw new IllegalStateException(err.toString());
                },
                result -> result
        );

        System.out.println(filter.test(json));
        System.out.println(filter.test(a));
        System.out.println(filter.test(second));
        System.out.println(filter.test(b));
    }
}
