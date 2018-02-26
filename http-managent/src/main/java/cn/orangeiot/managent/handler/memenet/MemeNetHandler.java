package cn.orangeiot.managent.handler.memenet;

import cn.orangeiot.common.genera.ErrorType;
import cn.orangeiot.common.genera.Result;
import cn.orangeiot.managent.handler.device.PublishDeviceHandler;
import cn.orangeiot.managent.verify.VerifyParamsUtil;
import cn.orangeiot.reg.EventbusAddr;
import cn.orangeiot.reg.memenet.MemenetAddr;
import cn.orangeiot.reg.user.UserAddr;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Objects;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-01-26
 */
public class MemeNetHandler implements EventbusAddr {

    private static Logger logger = LogManager.getLogger(MemeNetHandler.class);


    private EventBus eventBus;

    private JsonObject config;

    public MemeNetHandler(EventBus eventBus, JsonObject config) {
        this.eventBus = eventBus;
        this.config = config;
    }


    /**
     * @Description 米米网 用户批量注册接口
     * @author zhang bo
     * @date 18-1-26
     * @version 1.0
     */
    public void onRegisterUserBulk(RoutingContext routingContext) {
        logger.info("==MemeNetHandler=onRegisterUserBulk===params -> " + routingContext.getBodyAsString());
        eventBus.send(UserAddr.class.getName() + MEME_REGISTER_USER_BULK, "", rs -> {
            if (rs.failed()) {
                rs.cause().printStackTrace();
                routingContext.fail(501);
            } else {
                if (Objects.nonNull(rs.result().body()))
                    routingContext.response().end(JsonObject.mapFrom(new Result<>()).encodePrettily());
                else
                    routingContext.response().end(JsonObject.mapFrom(new Result<>()
                            .setErrorMessage(ErrorType.REGISTER_USER_BULK_FAIL.getKey()
                                    , ErrorType.REGISTER_USER_BULK_FAIL.getValue())).encodePrettily());
            }
        });
    }


}
