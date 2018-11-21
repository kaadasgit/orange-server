package cn.orangeiot.apidao.handler.dao.rateLimit;

import cn.orangeiot.apidao.client.RedisClient;
import cn.orangeiot.apidao.conf.RedisKeyConf;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import scala.util.parsing.json.JSONObject;

import java.lang.reflect.MalformedParametersException;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-08-27
 */
public class RateLimitDao {

    private static Logger logger = LogManager.getLogger(RateLimitDao.class);

    private final long addValue = 1;//递增单位


    /**
     * @Description 递增值+1 和设置有效时间
     * @author zhang bo
     * @date 18-11-21
     * @version 1.0
     */
    public void incrbyValAndExpire(Message<JsonObject> message) {
        RedisClient.client.incrby(RedisKeyConf.RATE_LIMIT + message.body().getString("uid"), addValue, rs -> {
            if (rs.failed()) logger.error(rs.cause().getMessage(), rs);
            else
                RedisClient.client.expire(RedisKeyConf.RATE_LIMIT + message.body().getString("uid"), message.body().getLong("time")
                        , time -> {
                            if (time.failed()) logger.error(time.cause().getMessage(), time);
                        });
        });
    }

    /**
     * @param message params | uid 用户id
     * @Description rust请求 加
     * @author zhang bo
     * @date 18-8-27
     * @version 1.0
     */
    public void rustRequestAdd(Message<JsonObject> message) {
        RedisClient.client.ttl(RedisKeyConf.RATE_LIMIT + message.body().getString("uid"), time -> {
            if (time.failed())
                logger.error(time.cause().getMessage(), time.cause());
            else {
                if (time.result() < 3) {//key 不存在
                    incrbyValAndExpire(message);
                } else {
                    RedisClient.client.incrby(RedisKeyConf.RATE_LIMIT + message.body().getString("uid"), addValue, rs -> {
                        if (rs.failed()) logger.error(rs.cause().getMessage(), rs);
                    });
                }
            }
        });
    }

}
