package cn.orangeiot.message.handler.notify;

import cn.orangeiot.common.options.SendOptions;
import cn.orangeiot.message.Model.MqttQos;
import cn.orangeiot.reg.message.MessageAddr;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.Objects;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-01-05
 */
public class NotifyHandler implements MessageAddr {

    private static Logger logger = LogManager.getLogger(NotifyHandler.class);

    private Vertx vertx;

    private JsonObject config;

    public NotifyHandler(Vertx vertx, JsonObject config) {
        this.config = config;
        this.vertx = vertx;
    }

    /**
     * @Description 通知网关管理员
     * @author zhang bo
     * @date 18-1-5
     * @version 1.0
     */
    public void notifyGatewayAdmin(Message<JsonObject> message) {
        logger.info("==NotifyHandler=NotifyGatewayAdmin===params -> " + message.body());
        //获取网关管理员信息
        vertx.eventBus().send(MessageAddr.class.getName() + GET_GATEWAY_ADMIN, message.body(), (AsyncResult<Message<JsonObject>> rs) -> {
            if (rs.failed()) {
                rs.cause().printStackTrace();
            } else {
                if (Objects.nonNull(rs.result())) {
                    vertx.eventBus().send(MessageAddr.class.getName() + SEND_ADMIN_MSG, rs.result().body().put("requestuid",
                            message.body().getString("uid")).put("func", "notifyApprovalBindGW"),
                            SendOptions.getInstance().addHeader("qos", "1").addHeader("uid", rs.result().body().getString("adminuid")));
                }
            }
        });
    }

    /**
     * @Description 回复网关绑定申请用户
     * @author zhang bo
     * @date 18-1-8
     * @version 1.0
     */
    public void replyGatewayUser(Message<JsonObject> message) {
        logger.info("==NotifyHandler=replyGatewayUser===params -> " + message.body());
        vertx.eventBus().send(MessageAddr.class.getName() + SEND_ADMIN_MSG, message.body().put("func", "replyApprovalBindGW"),
                SendOptions.getInstance().addHeader("qos", "1").addHeader("uid", "app:" + message.body().getString("requestuid")));
    }
}
