package cn.orangeiot.publish.service.impl;

import cn.orangeiot.common.genera.ErrorType;
import cn.orangeiot.common.options.SendOptions;
import cn.orangeiot.common.utils.DataType;
import cn.orangeiot.common.utils.UUIDUtils;
import cn.orangeiot.common.verify.VerifyParamsUtil;
import cn.orangeiot.publish.model.ResultInfo;
import cn.orangeiot.publish.service.MemeNetService;
import cn.orangeiot.reg.gateway.GatewayAddr;
import cn.orangeiot.reg.memenet.MemenetAddr;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

import java.util.Objects;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-12-20
 */
public class MemeNetSserviceImpl implements MemeNetService {

    private EventBus eb;

    public MemeNetSserviceImpl(EventBus eb) {
        this.eb = eb;
    }


    /**
     * @param jsonObject 數據
     * @param handler    回調處理
     * @Description 注冊米米網用戶
     * @author zhang bo
     * @date 18-12-20
     * @version 1.0
     */
    public void registerMeme0(JsonObject jsonObject, Handler<AsyncResult<JsonObject>> handler) {
        //同步米米米网注册
        String meUsername = UUIDUtils.getUUID();
        String mePassword = UUIDUtils.getUUID();
        eb.send(MemenetAddr.class.getName() + REGISTER_USER_CALLBACK, new JsonObject().put("username", meUsername)
                .put("password", mePassword).put("uid", jsonObject.getString("uid")), SendOptions.getInstance(), (AsyncResult<Message<Boolean>> rs) -> {
            if (rs.failed()) {
                handler.handle(Future.succeededFuture(JsonObject.mapFrom(new ResultInfo<>().setErrorMessage(ErrorType.REGISTER_MEME_FAIL.getKey()
                        , ErrorType.REGISTER_MEME_FAIL.getValue(),jsonObject.getString("func")))));
            } else {
                if (Objects.nonNull(rs.result()) && rs.result().body())
                    handler.handle(Future.succeededFuture(JsonObject.mapFrom(new ResultInfo<>().setData(new JsonObject().put("meUsername", meUsername)
                            .put("mePwd", mePassword)).setFunc(jsonObject.getString("func")))));
                else
                    handler.handle(Future.succeededFuture(JsonObject.mapFrom(new ResultInfo<>().setErrorMessage(ErrorType.REGISTER_MEME_FAIL.getKey()
                            , ErrorType.REGISTER_MEME_FAIL.getValue(),jsonObject.getString("func")))));
            }
        });//第三方数据同步
    }


    @Override
    public void registerMeme(JsonObject jsonObject, Handler<AsyncResult<JsonObject>> handler) {
        VerifyParamsUtil.verifyParams(jsonObject, new JsonObject().put("uid", DataType.STRING), res -> {
            if (res.failed()) {
                handler.handle(Future.succeededFuture(new JsonObject().put("code", ErrorType.RESULT_PARAMS_FAIL.getKey())
                        .put("msg", ErrorType.RESULT_PARAMS_FAIL.getValue())));
            } else {
                this.registerMeme0(res.result(), handler);
            }
        });
    }


    /**
     * @param jsonObject 数据
     * @param handler    回调处理
     * @param data       數據
     * @Description 綁定米米網設備
     * @author zhang bo
     * @date 18-12-20
     * @version 1.0
     */
    public void bindMeme0(JsonObject jsonObject, JsonObject data, Handler<AsyncResult<JsonObject>> handler) {
        //同步绑定关系到第三方
        ResultInfo result = new ResultInfo<>();
        eb.send(MemenetAddr.class.getName() + BIND_DEVICE_USER,
                new JsonObject().put("uid", jsonObject.getString("uid"))
                        .put("devicesn", jsonObject.getString("devuuid")), SendOptions.getInstance()
                , mimiResult -> {
                    if (!Objects.nonNull(mimiResult.result().body())) {
                        if (!mimiResult.result().headers().isEmpty()) {
                            result.setErrorMessage(Integer.parseInt(mimiResult.result().headers().get("code")),
                                    mimiResult.result().headers().get("msg"), jsonObject.getString("func"));
                            handler.handle(Future.succeededFuture(JsonObject.mapFrom(result)));
                        } else {
                            result.setErrorMessage(ErrorType.MIMI_BIND_GATEWAY_FAIL.getKey(), ErrorType.MIMI_BIND_GATEWAY_FAIL.getValue()
                                    , jsonObject.getString("func"));
                            handler.handle(Future.succeededFuture(JsonObject.mapFrom(result)));
                        }
                    } else {
                        if (data != null)
                            result.setData(data);
                        result.setFunc(jsonObject.getString("func"));
                        handler.handle(Future.succeededFuture(JsonObject.mapFrom(result)));
                    }
                });
    }


    @Override
    public void registerMemeAndBind(JsonObject jsonObject, Handler<AsyncResult<JsonObject>> handler) {
        VerifyParamsUtil.verifyParams(jsonObject, new JsonObject().put("uid", DataType.STRING).put("devuuid", DataType.STRING), res -> {
            if (res.failed()) {
                handler.handle(Future.succeededFuture(new JsonObject().put("code", ErrorType.RESULT_PARAMS_FAIL.getKey())
                        .put("msg", ErrorType.RESULT_PARAMS_FAIL.getValue())));
            } else {
                this.registerMeme0(res.result(), rs -> {//注册用户
                    if (rs.failed()) {
                        handler.handle(rs);
                    } else {
                        if (Objects.nonNull(rs.result().getValue("code")) && !rs.result().getString("code").equals("200")) {
                            handler.handle(rs);
                        } else {// 注冊成功
                            JsonObject jsonObject1 = rs.result().getJsonObject("data");
                            this.bindMeme0(res.result(), new JsonObject().put("meUsername", jsonObject1.getString("meUsername"))
                                    .put("mePwd", jsonObject1.getString("mePwd")), handler);//绑定设备
                        }
                    }
                });
            }
        });
    }

    @Override
    public void bindMeme(JsonObject jsonObject, Handler<AsyncResult<JsonObject>> handler) {
        VerifyParamsUtil.verifyParams(jsonObject, new JsonObject().put("uid", DataType.STRING).put("devuuid", DataType.STRING), res -> {
            if (res.failed()) {
                handler.handle(Future.succeededFuture(new JsonObject().put("code", ErrorType.RESULT_PARAMS_FAIL.getKey())
                        .put("msg", ErrorType.RESULT_PARAMS_FAIL.getValue())));
            } else {
                this.bindMeme0(jsonObject, null, handler);
            }
        });
    }
}
