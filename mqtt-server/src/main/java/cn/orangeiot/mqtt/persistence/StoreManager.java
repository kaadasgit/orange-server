package cn.orangeiot.mqtt.persistence;

import cn.orangeiot.mqtt.parser.MQTTDecoder;
import cn.orangeiot.mqtt.parser.MQTTEncoder;
import cn.orangeiot.reg.storage.StorageAddr;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dna.mqtt.moquette.proto.messages.PublishMessage;

import javax.mail.Store;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by giova_000 on 03/06/2015.
 */
public class StoreManager implements StorageAddr {

    private Vertx vertx;
    private MQTTEncoder encoder;
    private MQTTDecoder decoder;

    private static Logger logger = LogManager.getLogger(StoreManager.class);

    public StoreManager(Vertx vertx) {
        this.vertx = vertx;
        this.encoder = new MQTTEncoder();
        this.decoder = new MQTTDecoder();
    }


    public void saveRetainMessage(String tenant, PublishMessage pm) {
        try {
            String topic = pm.getTopicName();
            Buffer pmBytes = encoder.enc(pm);
            JsonObject request = new JsonObject()
                    .put("topic", topic)
                    .put("tenant", tenant)
                    .put("message", pmBytes.getBytes());
            vertx.eventBus().publish(
                    StoreVerticle.ADDRESS,
                    request,
                    new DeliveryOptions().addHeader("command", "saveRetainMessage"));

        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void deleteRetainMessage(String tenant, String topic) {
        try {
            JsonObject request = new JsonObject()
                    .put("topic", topic)
                    .put("tenant", tenant);
            vertx.eventBus().publish(
                    StoreVerticle.ADDRESS,
                    request,
                    new DeliveryOptions().addHeader("command", "deleteRetainMessage"));

        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void getRetainedMessagesByTopicFilter(String tenant, String topicFilter, Handler<List<PublishMessage>> handler) {
        List<PublishMessage> list = new ArrayList<>();

        JsonObject request = new JsonObject()
                .put("topicFilter", topicFilter)
                .put("tenant", tenant);
        vertx.eventBus().send(
                StoreVerticle.ADDRESS,
                request,
                new DeliveryOptions().addHeader("command", "getRetainedMessagesByTopicFilter"),
                (AsyncResult<Message<JsonObject>> res) -> {
                    if (res.succeeded()) {
                        Message<JsonObject> msg = res.result();
                        JsonObject response = msg.body();
                        JsonArray results = response.getJsonArray("results");
                        List<JsonObject> retained = (List<JsonObject>) results.getList();
                        int size = results.size();
                        for (int i = 0; i < size; i++) {
                            try {
                                JsonObject item = results.getJsonObject(i);
//                                String topic = item.getString("topic");
                                byte[] message = item.getBinary("message");
                                Buffer pmBytes = Buffer.buffer(message);
                                PublishMessage pm = (PublishMessage) decoder.dec(pmBytes);
                                list.add(pm);
                            } catch (Throwable e) {
                                logger.error(e.getMessage(), e);
                            }
                        }
                        handler.handle(list);
                    }
                });

    }

}
