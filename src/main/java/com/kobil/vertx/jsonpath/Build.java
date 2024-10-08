package com.kobil.vertx.jsonpath;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class Build {
    public static void main(String[] args) {
//        var jp = "$.a[1].b[1:3,4:-1:2]";

        var compiled = JsonPath.ROOT
                .field("a")
                .index(1)
                .field("b")
                .selectChildren(Selector.slice(1, 3), Selector.slice(4, -1, 2));

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

        var jp2 = JsonPath.ROOT.field("a");
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

//        var expr = "@.a";
        var filter = QueryExpression.relative().field("a").exists();
        System.out.println(filter);

        System.out.println(filter.test(json));
        System.out.println(filter.test(a));
        System.out.println(filter.test(second));
        System.out.println(filter.test(b));
    }
}
