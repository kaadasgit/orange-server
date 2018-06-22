package cn.orangeiot.apidao.client;

import cn.orangeiot.apidao.verticle.ApiDaoVerticle;
import cn.orangeiot.common.utils.UUIDUtils;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.RedisOptions;
import io.vertx.redis.impl.RedisCommand;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-11-23
 */
public class RedisClient {

    public static io.vertx.redis.RedisClient client;


    private static Logger logger = LogManager.getLogger(RedisClient.class);

    /**
     * r
     *
     * @Description redisClient配置
     * @author zhang bo
     * @date 17-11-23
     * @version 1.0
     */
    public void redisConf(Vertx vertx) {
        InputStream redisIn = RedisClient.class.getResourceAsStream("/redis-conf.json");
        String redisConf = "";//jdbc连接配置
        try {
            redisConf = IOUtils.toString(redisIn, "UTF-8");//获取配置
            if (!redisConf.equals("")) {
                JsonObject json = new JsonObject(redisConf);

                RedisOptions redisOptions = new RedisOptions().setHost(json.getString("host"))
                        .setPort(json.getInteger("port"));

                if (Objects.nonNull(json.getValue("password")))
                    redisOptions.setAuth(json.getString("password"));
                client = io.vertx.redis.RedisClient.create(vertx, redisOptions);//创建redisclient
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        } finally {
            if (null != redisIn)
                try {
                    redisIn.close();
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
        }
    }
}
