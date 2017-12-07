package cn.orangeiot.mqtt.verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Objects;

/**
 * @author zhang bo
 * @version 1.0
 * @Description 订阅处理测试
 * @date 2017-11-20
 */
public class SubscibeVerticle extends AbstractVerticle{

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        vertx.eventBus().consumer("mqtt.authenticator.oauth2.apifest.subscribe",rs->{
            if(Objects.nonNull(rs.body())){
                JsonObject jsonObjec=new JsonObject(rs.body().toString());
                rs.reply(new JsonObject().put("permitted",new JsonArray().add(true)));
            }
        });

        startFuture.complete();
    }

    @Override
    public void stop(Future<Void> stopFuture) throws Exception {
        super.stop(stopFuture);
    }
}
