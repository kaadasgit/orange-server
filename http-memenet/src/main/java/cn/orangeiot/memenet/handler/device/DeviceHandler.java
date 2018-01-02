package cn.orangeiot.memenet.handler.device;

import cn.orangeiot.common.utils.KdsCreateRandom;
import cn.orangeiot.common.utils.SHA256;
import cn.orangeiot.memenet.client.HttpClient;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-01-02
 */
public class DeviceHandler {

    private static Logger logger = LoggerFactory.getLogger(DeviceHandler.class);

    private JsonObject conf;

    public DeviceHandler(JsonObject conf) {
        this.conf = conf;
    }


    /**
     * @Description MIMI用户绑定设备
     * @author zhang bo
     * @date 18-1-2
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void onBindDeviceByUser(Message<JsonObject> message) {
        logger.info("==DeviceHandler=onBindDeviceByUser===params -> " + message.body());
        String random = KdsCreateRandom.createRandom(10);//获取随机数
        //TODO sha256加密
        SHA256.getSHA256Str(conf.getString("sig").replace("RANDOM_VALUE", random), as -> {
            if (as.failed()) {
                as.cause().printStackTrace();
                logger.error("==DeviceHandler=onBindDeviceByUser Usersha256 encrypt is fail");
            } else {
                //注册用户请求
                HttpClient.client.post("/v1/accsvr/binddevice")
                        .addQueryParam("partid", conf.getString("partid"))
                        .addQueryParam("appid", conf.getString("appId"))
                        .addQueryParam("random", random)
                        .sendJsonObject(new JsonObject().put("userid", message.body().getString("userid"))
                                .put("devicesn", message.body().getString("devicesn")).put("sig", as.result()), rs -> {
                            if (rs.failed()) {
                                rs.cause().printStackTrace();
                                logger.error("==DeviceHandler=onBindDeviceByUser===request /v1/accsvr/binddevice timeout");
                            } else {
                                logger.info("==DeviceHandler=onBindDeviceByUser===request /v1/accsvr/binddevice result -> " + rs.result().body());
                            }
                        });
            }
        });
    }


    /**
     * @Description MIMI用户解除设备绑定
     * @author zhang bo
     * @date 18-1-2
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void onRelieveDeviceByUser(Message<JsonObject> message) {
        logger.info("==DeviceHandler=onRelieveDeviceByUser===params -> " + message.body());
        String random = KdsCreateRandom.createRandom(10);//获取随机数
        //TODO sha256加密
        SHA256.getSHA256Str(conf.getString("sig").replace("RANDOM_VALUE", random), as -> {
            if (as.failed()) {
                as.cause().printStackTrace();
                logger.error("==DeviceHandler=onRelieveDeviceByUser Usersha256 encrypt is fail");
            } else {
                //注册用户请求
                HttpClient.client.post("/v1/accsvr/unbinddevice")
                        .addQueryParam("partid", conf.getString("partid"))
                        .addQueryParam("appid", conf.getString("appId"))
                        .addQueryParam("random", random)
                        .sendJsonObject(new JsonObject().put("userid", message.body().getString("userid"))
                                .put("devicesn", message.body().getString("devicesn")).put("sig", as.result()), rs -> {
                            if (rs.failed()) {
                                rs.cause().printStackTrace();
                                logger.error("==DeviceHandler=onRelieveDeviceByUser===request /v1/accsvr/unbinddevice timeout");
                            } else {
                                logger.info("==DeviceHandler=onRelieveDeviceByUser===request /v1/accsvr/unbinddevice result -> " + rs.result().body());
                            }
                        });
            }
        });
    }



    /**
     * @Description MIMI终端设备删除
     * @author zhang bo
     * @date 18-1-2
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void onDelDevice(Message<JsonObject> message) {
        logger.info("==DeviceHandler=onDelDevice===params -> " + message.body());
        String random = KdsCreateRandom.createRandom(10);//获取随机数
        //TODO sha256加密
        SHA256.getSHA256Str(conf.getString("sig").replace("RANDOM_VALUE", random), as -> {
            if (as.failed()) {
                as.cause().printStackTrace();
                logger.error("==DeviceHandler=onDelDevice Usersha256 encrypt is fail");
            } else {
                //注册用户请求
                HttpClient.client.post("/v1/accsvr/deldevice")
                        .addQueryParam("partid", conf.getString("partid"))
                        .addQueryParam("appid", conf.getString("appId"))
                        .addQueryParam("random", random)
                        .sendJsonObject(new JsonObject().put("devicesn", message.body().getString("devicesn"))
                                .put("sig", as.result()), rs -> {
                            if (rs.failed()) {
                                rs.cause().printStackTrace();
                                logger.error("==DeviceHandler=onDelDevice===request /v1/accsvr/deldevice timeout");
                            } else {
                                logger.info("==DeviceHandler=onDelDevice===request /v1/accsvr/deldevice result -> " + rs.result().body());
                            }
                        });
            }
        });
    }
}

