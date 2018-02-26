package cn.orangeiot.auth.handler.connect;


import cn.orangeiot.common.options.SendOptions;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.Objects;
import java.util.UUID;

/**
 * @author zhang bo  mqtt连接验证
 * @version 1.0
 * @Description
 * @date 2017-11-23
 */
public class ConnectHandler {

    private static Logger logger = LogManager.getLogger(ConnectHandler.class);


    private Vertx vertx;

    private JsonObject config;

    public ConnectHandler(Vertx vertx, JsonObject config) {
        this.vertx=vertx;
        this.config=config;
    }

    public void onMessage(Message message){
        if(Objects.nonNull(message.body())){
            logger.info("==ConnectHandler=onMessage=params:"+message.body());
            JsonObject jsonObject=new JsonObject(message.body().toString());

            /*查找*/
            vertx.eventBus().send(config.getString("send_connect_dao"),message.body(), SendOptions.getInstance(),(AsyncResult<Message<Boolean>> rs)->{
                if(rs.failed()){
                    rs.cause().printStackTrace();
                    message.reply(new JsonObject().put("token", UUID.randomUUID().toString()).put("authorized_user",
                            jsonObject.getString("username")).put("auth_valid", false));
                }else {
                    if (rs.result().body()) {//验证成功
                        message.reply(new JsonObject().put("token", UUID.randomUUID().toString()).put("authorized_user",
                                jsonObject.getString("username")).put("auth_valid", rs.result().body()));
                    } else {
                        message.reply(new JsonObject().put("token", UUID.randomUUID().toString()).put("authorized_user",
                                jsonObject.getString("username")).put("auth_valid", rs.result().body()));
                    }
                }
            });


        }
    }
}
