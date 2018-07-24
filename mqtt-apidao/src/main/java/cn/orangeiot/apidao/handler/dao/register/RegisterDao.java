package cn.orangeiot.apidao.handler.dao.register;

import cn.orangeiot.apidao.client.RedisClient;
import cn.orangeiot.apidao.conf.RedisKeyConf;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Objects;

public class RegisterDao {

    private static Logger logger = LogManager.getLogger(RegisterDao.class);


    /**
     * @Description 保存注册信息
     * @author zhang bo
     * @date 18-3-8
     * @version 1.0
     */
    public void saveRegisterInfo(Message<JsonObject> message) {
        RedisClient.client.hset(RedisKeyConf.REGISTER_USER, message.body().getString("uri"), message.body().getString("socketAddress"), rs -> {
            if (rs.failed()) {
                logger.error(rs.cause().getMessage(), rs.cause());
            }
        });
    }


    /**
     * @Description 獲取注册信息
     * @author zhang bo
     * @date 18-3-8
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void getRegisterInfo(Message<String> message) {
        RedisClient.client.hget(RedisKeyConf.REGISTER_USER, message.body(), rs -> {
            if (rs.failed()) {
                logger.error(rs.cause().getMessage(), rs.cause());
            } else {
                if (Objects.nonNull(rs.result()))
                    message.reply(rs.result());
                else
                    message.reply(null);
            }
        });
    }


    /**
     * @Description 删除注册信息
     * @author zhang bo
     * @date 18-3-8
     * @version 1.0
     */
    public void delRegisterInfo(Message<JsonObject> message) {
        RedisClient.client.hdel(RedisKeyConf.REGISTER_USER, message.body().getString("uri"), rs -> {
            if (rs.failed())
                logger.error(rs.cause().getMessage(), rs.cause());
        });
    }


    /**
     * @Description 保存会话的映射地址
     * @author zhang bo
     * @date 18-3-8
     * @version 1.0
     */
    public void saveCallIdAddr(Message<JsonObject> message) {
        RedisClient.client.hget(RedisKeyConf.REGISTER_USER, message.body().getString("sendAddr"), rs -> {
            if (rs.failed()) {
                logger.error(rs.cause().getMessage(), rs.cause());
            } else {
                if (Objects.nonNull(rs.result())) {
                    RedisClient.client.hset(RedisKeyConf.CALLIDADDR, message.body().getString("mediaType") + rs.result().split(":")[0], message.body().getString("receAddr"), as -> {
                        if (as.failed()) {
                            as.cause().printStackTrace();
                        }
                    });
                }
            }
        });
    }


    /**
     * @Description 獲取会话的映射地址
     * @author zhang bo
     * @date 18-3-8
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void getCallIdAddr(Message<String> message) {
        RedisClient.client.hget(RedisKeyConf.CALLIDADDR, message.body(), rs -> {
            if (rs.failed()) {
                logger.error(rs.cause().getMessage(), rs.cause());
            } else {
                if (Objects.nonNull(rs.result()))
                    message.reply(rs.result());
                else
                    message.reply(null);
            }
        });
    }

}
