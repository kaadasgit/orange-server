package cn.orangeiot.mqtt.verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

import java.util.Objects;
import java.util.UUID;

/**
 * @author zhang bo
 * @version 1.0
 * @Description 发送主题测试
 * @date 2017-11-20
 */
public class AuthServerVerticle extends AbstractVerticle{

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        vertx.eventBus().consumer("mqtt.authenticator.oauth2.apifest",rs -> {
             if(Objects.nonNull(rs.body())){
                 JsonObject jsonObject=new JsonObject(rs.body().toString());

                 rs.reply(new JsonObject().put("token", UUID.randomUUID().toString()).put("authorized_user",
                         jsonObject.getString("username")).put("auth_valid",true));

             }
        });

        startFuture.complete();
    }

    @Override
    public void stop(Future<Void> stopFuture) throws Exception {
        super.stop(stopFuture);
    }
}
