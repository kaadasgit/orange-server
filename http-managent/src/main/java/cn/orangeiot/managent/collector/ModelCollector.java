package cn.orangeiot.managent.collector;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * @author zhang bo
 * @version 1.0
 * @Description  所有產品類型收集器
 * @date 2018-03-30
 */
public class ModelCollector implements Collector<JsonObject
        , JsonArray, JsonArray> {

    @Override
    public Supplier<JsonArray> supplier() {
        return () -> new JsonArray();
    }

    @Override
    public BiConsumer<JsonArray, JsonObject> accumulator() {
        return (JsonArray acc, JsonObject con) -> {
            JsonObject map = (JsonObject) acc.stream().filter(e ->
                    new JsonObject(e.toString()).getString("modelCode").equals(con.getString("modelCode")))
                    .findFirst().orElse(null);
            if (Objects.nonNull(map)) {
                map.getJsonArray("childCodes").add(con.getString("childCode"));
            } else {
                con.put("childCodes", new JsonArray().add(con.getString("childCode")));
                con.remove("childCode");
                acc.add(con);
            }
        };
    }


    @Override
    public BinaryOperator<JsonArray> combiner() {
        return null;
    }

    @Override
    public Function<JsonArray, JsonArray> finisher() {
        return Function.identity();
    }

    @Override
    public Set<Characteristics> characteristics() {
        return Collections.unmodifiableSet(EnumSet.of(Characteristics.IDENTITY_FINISH));
    }
}
