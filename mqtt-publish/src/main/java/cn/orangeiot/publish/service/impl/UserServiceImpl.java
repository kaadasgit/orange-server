package cn.orangeiot.publish.service.impl;

import cn.orangeiot.common.genera.ErrorType;
import cn.orangeiot.publish.handler.message.FuncHandler;
import cn.orangeiot.publish.service.UserService;
import cn.orangeiot.reg.message.MessageAddr;
import cn.orangeiot.reg.user.UserAddr;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Objects;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-05-04
 */
public class UserServiceImpl implements UserService {


    private static Logger logger = LogManager.getLogger(UserServiceImpl.class);

    private Vertx vertx;

    private JsonObject conf;

    public UserServiceImpl(Vertx vertx, JsonObject conf) {
        this.vertx = vertx;
        this.conf = conf;
    }

    /**
     * @Description 获取网关管理员
     * @author zhang bo
     * @date 18-5-4
     * @version 1.0
     */
    @Override
    public void selectGWAdmin(JsonObject jsonObject) {
        logger.info("params -> {}", jsonObject);
        vertx.eventBus().send(UserAddr.class.getName() + GET_GW_ADMIN, jsonObject, rs -> {
            if (rs.failed()) {
                rs.cause().printStackTrace();
            } else {
                DeliveryOptions deliveryOptions = new DeliveryOptions().addHeader("uid"
                        , "gw:" + jsonObject.getString("gwId")).addHeader("qos", "1")
                        .addHeader("topic", MessageAddr.SEND_GATEWAY_REPLAY.replace("gwId", jsonObject.getString("gwId")));

                if (Objects.nonNull(rs.result().body())) {
                    vertx.eventBus().send(MessageAddr.class.getName() + SEND_UPGRADE_MSG, jsonObject
                                    .put("returnCode", 200).put("returnData", rs.result().body())
                            , deliveryOptions);
                } else {
                    vertx.eventBus().send(MessageAddr.class.getName() + SEND_UPGRADE_MSG, jsonObject
                                    .put("returnCode", ErrorType.RESULT_RESOURCES_NOT_FIND.getKey()).put("returnData", new JsonObject())
                            , deliveryOptions);
                }
            }
        });

    }
}
