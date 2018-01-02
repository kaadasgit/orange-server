package cn.orangeiot.apidao.client;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.RedisOptions;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-11-23
 */
public class RedisClient {

    public static io.vertx.redis.RedisClient client;

    /**r
     * @Description redisClient配置
     * @author zhang bo
     * @date 17-11-23
     * @version 1.0
     */
    public void redisConf(Vertx vertx){
        InputStream redisIn = RedisClient.class.getResourceAsStream("/redis-conf.json");
        String redisConf = "";//jdbc连接配置
        try {
            redisConf = IOUtils.toString(redisIn, "UTF-8");//获取配置
            if (!redisConf.equals("")) {
                JsonObject json = new JsonObject(redisConf);

                client = io.vertx.redis.RedisClient.create(vertx, new RedisOptions().setHost(json.getString("host"))
                        .setPort(json.getInteger("port")).setAuth(json.getString("password")));//创建redisclient
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != redisIn)
                try {
                    redisIn.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }
}
