package cn.orangeiot.http.handler.mac;

import cn.orangeiot.common.genera.Result;
import cn.orangeiot.common.options.SendOptions;
import cn.orangeiot.common.utils.DataType;
import cn.orangeiot.http.verify.VerifyParamsUtil;
import cn.orangeiot.reg.adminlock.AdminlockAddr;
import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
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
public class MacHandler implements AdminlockAddr {
    private static Logger logger = LogManager.getLogger(MacHandler.class);

    private EventBus eventBus;

    private JsonObject config;

    public MacHandler(EventBus eventBus, JsonObject config) {
        this.eventBus = eventBus;
        this.config = config;
    }


    /**
     * @Description 获取mac地址
     * @author zhang bo
     * @date 18-1-26
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void getMacAddr(RoutingContext routingContext) {
        logger.info("==LockHandler=getMacAddr==params->" + routingContext.getBodyAsString());
        //验证参数的合法性
        VerifyParamsUtil.verifyParams(routingContext, new JsonObject().put("SN", DataType.STRING), asyncResult -> {
            if (asyncResult.failed()) {
                routingContext.fail(401);
            } else {
                eventBus.send(AdminlockAddr.class.getName() + GET_MODEL_PASSWORD, asyncResult.result(), SendOptions.getInstance()
                        , (AsyncResult<Message<JsonObject>> rs) -> {
                            if (rs.failed()) {
                                routingContext.fail(501);
                            } else {
                                if (Objects.nonNull(rs.result().body())) {
                                    routingContext.response().end(JsonObject.mapFrom(new Result<JsonObject>().setData(rs.result().body())).toString());
                                } else {
                                    if (!rs.result().headers().isEmpty())
                                        routingContext.response().end(JsonObject.mapFrom(
                                                new Result<>().setErrorMessage(Integer.parseInt(rs.result().headers().get("code")), rs.result().headers().get("msg"))).toString());
                                    else
                                        routingContext.fail(501);
                                }
                            }
                        });
            }
        });
    }
}
