package cn.orangeiot.memenet.handler.device;

import cn.orangeiot.common.options.SendOptions;
import cn.orangeiot.common.utils.KdsCreateRandom;
import cn.orangeiot.common.utils.SHA256;
import cn.orangeiot.memenet.client.HttpClient;
import cn.orangeiot.reg.gateway.GatewayAddr;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import io.vertx.ext.web.codec.BodyCodec;

import java.util.Objects;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-01-02
 */
public class DeviceHandler implements GatewayAddr {

    private static Logger logger = LogManager.getLogger(DeviceHandler.class);

    private JsonObject conf;

    private Vertx vertx;

    public DeviceHandler(JsonObject conf, Vertx vertx) {
        this.conf = conf;
        this.vertx = vertx;
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
                vertx.eventBus().send(GatewayAddr.class.getName() + GET_USERINFO, message.body(), SendOptions.getInstance()
                        , (AsyncResult<Message<JsonObject>> ers) -> {
                            if (ers.failed()) {
                                logger.error(ers.cause().getMessage(), ers.cause());
                                logger.error("==DeviceHandler=onBindDeviceByUser===501");
                            } else {
                                //注册用户请求
                                HttpClient.client.post("/v1/accsvr/binddevice")
                                        .as(BodyCodec.jsonObject())
                                        .addQueryParam("partid", conf.getString("partid"))
                                        .addQueryParam("appid", conf.getString("appId"))
                                        .addQueryParam("random", random)
                                        .sendJsonObject(new JsonObject().put("userid", ers.result().body().getLong("userid").toString())
                                                .put("devicesn", message.body().getString("devicesn")).put("sig", as.result()), rs -> {
                                            if (rs.failed()) {
                                                logger.error(rs.cause().getMessage(), rs.cause());
                                                logger.error("==DeviceHandler=onBindDeviceByUser===request /v1/accsvr/binddevice timeout");
                                                message.reply(null);
                                                vertx.eventBus().send(GatewayAddr.class.getName() + UPDATE_GATEWAY_DOMAIN, new JsonObject()
                                                        .put("type", 0)
                                                        .put("userid", ers.result().body().getLong("userid").toString())
                                                        .put("devicesn", message.body().getString("devicesn")), SendOptions.getInstance());
                                            } else {
                                                logger.info("==DeviceHandler=onBindDeviceByUser===request /v1/accsvr/binddevice result -> " + rs.result().body());
                                                if (rs.result().body().getInteger("result") == 0) {
                                                    vertx.eventBus().send(GatewayAddr.class.getName() + UPDATE_GATEWAY_DOMAIN, new JsonObject()
                                                            .put("devicesn", message.body().getString("devicesn"))
                                                            .put("domain", rs.result().body().getString("domain")), SendOptions.getInstance());
                                                    message.reply(new JsonObject());
                                                } else {
                                                    logger.error("==DeviceHandler=onBindDeviceByUser===request /v1/accsvr/binddevice result ->" + rs.result().body().getInteger("result"));
                                                    vertx.eventBus().send(GatewayAddr.class.getName() + UPDATE_GATEWAY_DOMAIN, new JsonObject()
                                                            .put("type", 0)
                                                            .put("userid", ers.result().body().getLong("userid").toString())
                                                            .put("devicesn", message.body().getString("devicesn")), SendOptions.getInstance());
                                                    message.reply(null);
                                                }

                                            }
                                        });
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
                logger.error(as.cause().getMessage(), as.cause());
                logger.error("==DeviceHandler=onRelieveDeviceByUser Usersha256 encrypt is fail");
            } else {
                vertx.eventBus().send(GatewayAddr.class.getName() + GET_GATEWWAY_USERID_LIST, message.body(), (AsyncResult<Message<JsonArray>> drs) -> {
                    if (drs.failed()) {
                        logger.error(drs.cause().getMessage(), drs.cause());
                    } else {
                        if (Objects.nonNull(drs.result())) {
                            drs.result().body().forEach(e -> {
                                HttpClient.client.post("/v1/accsvr/unbinddevice")
                                        .addQueryParam("partid", conf.getString("partid"))
                                        .addQueryParam("appid", conf.getString("appId"))
                                        .addQueryParam("random", random)
                                        .sendJsonObject(new JsonObject().put("userid", new JsonObject(e.toString()).getLong("userid"))
                                                .put("devicesn", message.body().getString("devuuid")).put("sig", as.result()), rs -> {
                                            if (rs.failed()) {
                                                logger.error(rs.cause().getMessage(), rs.cause());
                                                logger.error("==DeviceHandler=onRelieveDeviceByUser===request /v1/accsvr/unbinddevice timeout");
                                            } else {
                                                logger.info("==DeviceHandler=onRelieveDeviceByUser===request /v1/accsvr/unbinddevice result -> " + rs.result().body());
                                            }
                                        });
                            });
                        }
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
                logger.error(as.cause().getMessage(), as.cause());
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
                                logger.error(rs.cause().getMessage(), rs.cause());
                                logger.error("==DeviceHandler=onDelDevice===request /v1/accsvr/deldevice timeout");
                            } else {
                                logger.info("==DeviceHandler=onDelDevice===request /v1/accsvr/deldevice result -> " + rs.result().body());
                            }
                        });
            }
        });
    }


    /**
     * @Description 管理员删除普通用户解绑关系
     * @author zhang bo
     * @date 18-1-15
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void onDelGatewayByUser(Message<JsonObject> message) {
        logger.info("==DeviceHandler=onDelGatewayByUser===params -> " + message.body());
        String random = KdsCreateRandom.createRandom(10);//获取随机数
        //TODO sha256加密
        SHA256.getSHA256Str(conf.getString("sig").replace("RANDOM_VALUE", random), as -> {
            if (as.failed()) {
                logger.error(as.cause().getMessage(), as.cause());
                logger.error("==DeviceHandler=onDelGatewayByUser Usersha256 encrypt is fail");
            } else {
                //注册用户请求
                HttpClient.client.post("/v1/accsvr/unbinddevice")
                        .addQueryParam("partid", conf.getString("partid"))
                        .addQueryParam("appid", conf.getString("appId"))
                        .addQueryParam("random", random)
                        .sendJsonObject(new JsonObject().put("userid", message.body().getLong("userid"))
                                .put("devicesn", message.body().getString("devuuid")).put("sig", as.result()), rs -> {
                            if (rs.failed()) {
                                logger.error(rs.cause().getMessage(), rs.cause());
                                logger.error("==DeviceHandler=onDelGatewayByUser===request /v1/accsvr/unbinddevice timeout");
                            } else {
                                logger.info("==DeviceHandler=onDelGatewayByUser===request /v1/accsvr/unbinddevice result -> " + rs.result().body());
                            }
                        });
            }
        });
    }
}

