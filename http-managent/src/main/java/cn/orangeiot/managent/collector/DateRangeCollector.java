package cn.orangeiot.managent.collector;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * @author zhang bo
 * @version 1.0
 * @Description 時間範圍收集器
 * @date 2018-03-30
 */
public class DateRangeCollector implements Collector<JsonObject
        , JsonArray, JsonArray> {

    @Override
    public Supplier<JsonArray> supplier() {
        return () -> new JsonArray();
    }

    @SuppressWarnings("Duplicates")
    @Override
    public BiConsumer<JsonArray, JsonObject> accumulator() {
        return (JsonArray acc, JsonObject con) -> {
            JsonObject map = (JsonObject) acc.stream().filter(e ->
                    new JsonObject(e.toString()).getString("yearCode").equals(con.getString("yearCode")))
                    .findFirst().orElse(null);
            if (Objects.nonNull(map)) {
                Object params = map.getJsonArray("weekCodes").stream().filter(e -> e.equals(con.getString("weekCode")))
                        .findFirst().orElse(null);
                if (!Objects.nonNull(params))
                    map.getJsonArray("weekCodes").add(con.getString("weekCode"));
            } else {
                con.put("weekCodes", new JsonArray().add(con.getString("weekCode")));
                con.remove("weekCode");
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
