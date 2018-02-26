package cn.orangeiot.mqtt.rest;

import cn.orangeiot.mqtt.MQTTSession;
import cn.orangeiot.mqtt.parser.MQTTEncoder;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import org.dna.mqtt.moquette.proto.messages.PublishMessage;

import java.nio.ByteBuffer;

/**
 * Created by giovanni on 05/01/17.
 */
public class RestApiVerticle extends AbstractVerticle {

    private Logger logger = LogManager.getLogger(RestApiVerticle.class);

    @Override
    public void start() throws Exception {
        JsonObject restServerConf = config().getJsonObject("rest_server", new JsonObject());
        int httpPort = restServerConf.getInteger("port", 2883);

        String mqttAddress = MQTTSession.ADDRESS;

        HttpServer server = vertx.createHttpServer();
        Router router = Router.router(vertx);

        long size1mb = 1024*1024 ; //bytes
        router.route().handler(BodyHandler.create().setBodyLimit(size1mb));

        // http://<host:port>/pubsub/publish?channel=&lt;channel1&gt;&qos=0&retained=0
        // qos: MOST_ONE, LEAST_ONE, EXACTLY_ONC
        router.post("/pubsub/publish").handler( req -> {
            MultiMap headers = req.request().headers();
            MultiMap params = req.request().params();
            String tenant;
            if(headers.contains("tenant")) {
                tenant = headers.get("tenant");
            } else {
                tenant = params.get("tenant");
            }
            String topic;
            if(params.contains("topic")) {
                topic = req.request().params().get("topic");
            } else if (params.contains("channel")) {
                topic = req.request().params().get("channel");
            } else {
                throw new IllegalArgumentException("parameter 'topic' is required");
            }

            String qos = req.request().params().get("qos");
            String retained = req.request().params().get("retained");

            PublishMessage msg = new PublishMessage();
            msg.setMessageID(1);
            msg.setTopicName(topic);
            if ( qos != null) {
                AbstractMessage.QOSType theqos =
                        AbstractMessage.QOSType.valueOf(qos);
                msg.setQos(theqos);
            }
            if (retained != null)
                msg.setRetainFlag(true);

            try {
                Buffer body = req.getBody();
                byte[] payload = body.getBytes();
                msg.setPayload(ByteBuffer.wrap(payload));
                MQTTEncoder enc = new MQTTEncoder();
                DeliveryOptions opt = new DeliveryOptions()
                        .addHeader(MQTTSession.TENANT_HEADER, tenant);
                vertx.eventBus().publish(mqttAddress, enc.enc(msg), opt);
                req.response().setStatusCode(200);
            } catch (Throwable e) {
                logger.error(e.getMessage(), e);
                req.response().setStatusCode(500);
                req.response().setStatusMessage(e.getMessage());
            }
            req.response().end();
        });


        // http://<host:port>/pubsub/publish?channel=&lt;channel1&gt;&qos=0&retained=0
        // qos: MOST_ONE, LEAST_ONE, EXACTLY_ONC
        router.get("/get/topic").handler( req -> {
            req.response().end(JsonObject.mapFrom(MQTTSession.suscribeMap).encodePrettily());
        });


        router.exceptionHandler(event -> {
            logger.error(event.getMessage(), event.getCause());
        });


        server.requestHandler(router::accept).listen(httpPort, event -> {
            if (event.succeeded()) {
                logger.info("RestApiVerticle http server started on http://<host>:" + server.actualPort());
            } else {
                logger.info("RestApiVerticle http server NOT started !");
            }
        });
        logger.info("RestApiVerticle started" );
    }
}
