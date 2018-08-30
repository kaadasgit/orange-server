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
     * @param message params | uid 用户id
     * @Description rust请求 加
     * @author zhang bo
     * @date 18-8-27
     * @version 1.0
     */
    public void rustRequestAdd(Message<JsonObject> message) {
        RedisClient.client.exists(RedisKeyConf.RATE_LIMIT + message.body().getString("uid"), exists -> {
            if (exists.failed())
                logger.error(exists.cause().getMessage(), exists.cause());
            else {
                if (exists.result() > 0) {//存在
                    RedisClient.client.incrby(RedisKeyConf.RATE_LIMIT + message.body().getString("uid"), addValue, rs -> {
                                if (rs.failed()) logger.error(rs.cause().getMessage(), rs);
                            });
                }else{
                    RedisClient.client.incrby(RedisKeyConf.RATE_LIMIT + message.body().getString("uid"), addValue, rs -> {
                        if (rs.failed()) logger.error(rs.cause().getMessage(), rs);
                    });
                    RedisClient.client.expire(RedisKeyConf.RATE_LIMIT + message.body().getString("uid"), message.body().getLong("time")
                            , time -> {
                                if (time.failed()) logger.error(time.cause().getMessage(), time);
                            });
                }
            }
        });
    }

}
