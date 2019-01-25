package cn.orangeiot.apidao.handler.dao.job;

import cn.orangeiot.apidao.client.RedisClient;
import cn.orangeiot.apidao.conf.RedisKeyConf;
import com.sun.corba.se.impl.protocol.giopmsgheaders.MessageBase;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.op.SetOptions;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-12-18
 */
public class JobDao {

    private static Logger logger = LogManager.getLogger(JobDao.class);


    /**
     * @Description 重置验证码次数
     * @author zhang bo
     * @date 17-12-18
     * @version 1.0
     */
    public void onMsgVerifyCodeCount(Message<String> message) {
        RedisClient.client.del(RedisKeyConf.VERIFY_CODE_COUNT, rs -> {
            if (rs.failed()) logger.error(rs.cause().getMessage(), rs.cause());
        });
    }

    /**
     * @param message
     * @Description 保存个推auth_token
     */
    public void saveGtAuthtoken(Message<JsonObject> message) {
        SetOptions options = new SetOptions();
        options.setEX(24 * 60 * 60);
        RedisClient.client.setWithOptions(RedisKeyConf.GT_AUTHTOKEN + "_" + message.body().getString("appId")
                , message.body().getString("auth_token"), options, rs -> {
                    if (rs.failed()) {
                        logger.error(rs.cause().getMessage(), rs.cause());
                    }else {
                        logger.info("redis save gt_authtoken, appId -> {};authtoken -> {}", message.body().getString("appId"),message.body().getString("auth_token"));
                    }
                });
    }

    /**
     * @param message
     * @Description 获取个推auth_token
     */
    public void getGtAuthtoken(Message<String> message) {
        RedisClient.client.get(RedisKeyConf.GT_AUTHTOKEN + "_" + message.body().toString(), rs -> {
            if (rs.failed()) {
                logger.error(rs.cause().getMessage(), rs.cause());
            } else {
                message.reply(rs.result());
            }
        });
    }
}
