package cn.orangeiot.message.handler.ota;

import cn.orangeiot.message.handler.notify.NotifyHandler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Objects;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-04-04
 */
public class OtaUpgradeHandler {

    private static Logger logger = LogManager.getLogger(OtaUpgradeHandler.class);

    private Vertx vertx;

    private JsonObject config;

    public OtaUpgradeHandler(Vertx vertx, JsonObject config) {
        this.config = config;
        this.vertx = vertx;
    }


    /**
     * @Description OTA升级处理
     * @author zhang bo
     * @date 18-4-4
     * @version 1.0
     */
    public void UPgradeProcess(Message<JsonObject> message){
        logger.info("==UPgradeProcess==params -> {}", message.body());

        if(Objects.nonNull(message.body().getValue("type"))) {
            switch (message.body().getString("type")) {

            }
        }
    }



}
