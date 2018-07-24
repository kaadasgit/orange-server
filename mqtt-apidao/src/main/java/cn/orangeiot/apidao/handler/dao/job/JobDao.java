package cn.orangeiot.apidao.handler.dao.job;

import cn.orangeiot.apidao.client.RedisClient;
import cn.orangeiot.apidao.conf.RedisKeyConf;
import io.vertx.core.eventbus.Message;
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
    public void onMsgVerifyCodeCount(Message<String> message){
        RedisClient.client.del(RedisKeyConf.VERIFY_CODE_COUNT,rs->{
            if(rs.failed())logger.error(rs.cause().getMessage(), rs.cause());
        });
    }

}
