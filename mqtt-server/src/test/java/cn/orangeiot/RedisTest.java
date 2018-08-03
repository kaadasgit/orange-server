package cn.orangeiot;

import cn.orangeiot.mqtt.log.model.RedisKey;
import com.sun.org.apache.regexp.internal.RE;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.RedisClient;
import io.vertx.redis.RedisOptions;
import io.vertx.redis.op.ScanOptions;
import org.apache.commons.io.IOUtils;
import scala.util.parsing.json.JSONArray;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-07-31
 */
public class RedisTest {

    private RedisClient redisClient;

    public static void main(String[] args) {
        RedisTest redisTest = new RedisTest();
        redisTest.redisClientConf(Vertx.vertx());
//        for (int i = 0; i < 10; i++) {
//            redisTest.redisClient.hset("aqwesd", "1" + i, "cc" + i, rs -> {
//                if (rs.failed()) rs.cause().printStackTrace();
//            });
//        }

        redisTest.redisClient.hscan("123", "0", new ScanOptions().setCount(20), rs -> {
            if (rs.failed()) {
                rs.cause().printStackTrace();
            } else {
                System.out.println(rs.result().getJsonArray(1).size());
                System.out.println(rs.result().toString());
//                redisTest.notifyMsg(rs.result());
            }
        });
    }


    public void notifyMsg(JsonArray jsonArray) {
        if (jsonArray.size() > 0) {
            String custor = jsonArray.getString(0);
            JsonArray jsonArray1 = jsonArray.getJsonArray(1);
            System.out.println("val ============="+jsonArray1.size());
            jsonArray1.forEach(System.out::print);
            JsonArray jsons = new JsonArray();
            for (int i = 1; i < jsonArray1.size(); i = i + 2) {
                jsons.add(jsonArray1.getString(i).split(":")[1]);
            }
            System.out.println("val ============="+jsons.size());
            jsons.forEach(System.out::print);
        }

    }


    public void redisClientConf(Vertx vertx) {
        RedisOptions redisOptions = new RedisOptions().setHost("121.201.57.214")
                .setPort(6379);
        redisOptions.setAuth("zgh564739784");
        redisClient = io.vertx.redis.RedisClient.create(vertx, redisOptions);//创建redisclient
    }
}
