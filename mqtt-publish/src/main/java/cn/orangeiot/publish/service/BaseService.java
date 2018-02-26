package cn.orangeiot.publish.service;

import cn.orangeiot.common.genera.ErrorType;
import cn.orangeiot.common.options.SendOptions;
import cn.orangeiot.publish.handler.message.FuncHandler;
import cn.orangeiot.publish.model.ResultInfo;
import cn.orangeiot.reg.adminlock.AdminlockAddr;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.Objects;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-12-22
 */
public abstract class BaseService {

    private Vertx vertx;

    private JsonObject jsonObject;

    public BaseService(Vertx vertx, JsonObject jsonObject) {
        this.vertx = vertx;
        this.jsonObject = jsonObject;
    }

    /**
     * @Description 发送业务处理
     * @author zhang bo
     * @date 17-12-22
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void send(String addr, JsonObject jsonObject, Handler<AsyncResult<JsonObject>> handler) {
        vertx.eventBus().send(addr, jsonObject, SendOptions.getInstance(),(AsyncResult<Message<Object>> rs) -> {
            if (rs.failed()) {
                rs.cause().printStackTrace();
            } else {
                ResultInfo<Object> result = new ResultInfo<>();
                if (Objects.nonNull(rs.result().body())) {
                    if (!rs.result().headers().isEmpty()) {
                        result.setErrorMessage(Integer.parseInt(rs.result().headers().get("code")), rs.result().headers().get("msg")
                                , jsonObject.getString("func"));
                    } else {
                        result.setData(rs.result().body()).setFunc(jsonObject.getString("func"));
                    }
                    handler.handle(Future.succeededFuture(JsonObject.mapFrom(result)));
                } else {
                    result.setErrorMessage(ErrorType.OPERATION_FAIL.getKey(), ErrorType.OPERATION_FAIL.getValue()
                            , jsonObject.getString("func"));
                    handler.handle(Future.succeededFuture(JsonObject.mapFrom(result)));
                }
            }
        });
    }

}
