package cn.orangeiot.handler.connect;

import cn.orangeiot.client.MongoClient;
import cn.orangeiot.utils.GUID;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.Objects;
import java.util.UUID;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-11-27
 */
public class ConnectHandler {
    private static Logger logger = LoggerFactory.getLogger(ConnectHandler.class);


    private Vertx vertx;

    private JsonObject config;

    public ConnectHandler(Vertx vertx,JsonObject config) {
        this.vertx=vertx;
        this.config=config;
    }


    /**
     * @Description 获取用户
     * @author zhang bo
     * @date 17-11-27
     * @version 1.0
     */
    public void getUser(Message<JsonObject> message){
        MongoClient.client.findOne("sys_user",new JsonObject().put("account", message.body().getString("username")), new JsonObject()
                .put("type", "").put("status", "").put("salt", "").put("password", ""),res->{
            if (res.failed()) {
                res.cause().printStackTrace();
            } else {
                   if(Objects.nonNull(res.result())
                           && GUID.MD5(message.body().getString("password") + res.result().getString("salt").toString()).equals((res.result().getString("password").toString()))){
                         message.reply(new JsonObject().put("password",res.result().getString("password")).put("salt"
                         ,res.result().getString("salt")).put("username",message.body().getString("username")).put("code",true));
                   }else{
                       message.reply(new JsonObject().put("code",false));
                   }
            }
        });
    }

}
