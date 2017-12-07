package cn.orangeiot.handler.connect;

import cn.orangeiot.client.RedisClient;
import cn.orangeiot.conf.RedisKeyConf;
import cn.orangeiot.utils.GUID;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.Objects;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-11-27
 */
public class ConnectHandler {

    private static Logger logger = LoggerFactory.getLogger(ConnectHandler.class);


    public void getUser(Message<JsonObject> message){
        RedisClient.client.hget(RedisKeyConf.userAccount,message.body().getString("username"),rs->{
            if(rs.failed()){
                rs.cause().printStackTrace();
            }else{
                if(Objects.nonNull(rs.result()) && GUID.MD5(message.body().getString("password")+
                rs.result().split("::")[0]).equals(rs.result().split("::")[1])){
                    message.reply(true);
                }else{
                    message.reply(false);
                }
            }
        });

    }
}
