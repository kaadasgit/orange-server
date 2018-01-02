package cn.orangeiot;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import org.omg.Messaging.SYNC_WITH_TRANSPORT;
import org.w3c.dom.ranges.Range;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-01-02
 */
public class EventbusTest {

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.eventBus().consumer("cn.test", rs -> {
            vertx.executeBlocking(future -> {
                for (int i = 0; i < 100000000000000L; i++) {

                }
                future.complete(new JsonObject().put("code", 200));
            }, as -> {
                rs.reply(as.result());
            });

        });

        vertx.eventBus().send("cn.test", new JsonObject().put("code", "123"),new DeliveryOptions().setSendTimeout(2000),rs -> {
            if (rs.failed()) {
                rs.cause().printStackTrace();
            } else {
                System.out.println("=========result== params -> " + rs.result().body());
            }
        });
    }
}
