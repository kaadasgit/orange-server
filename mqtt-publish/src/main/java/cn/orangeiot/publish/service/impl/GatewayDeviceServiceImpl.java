package cn.orangeiot.publish.service.impl;

import cn.orangeiot.common.genera.ErrorType;
import cn.orangeiot.common.genera.Result;
import cn.orangeiot.common.options.SendOptions;
import cn.orangeiot.common.utils.DataType;
import cn.orangeiot.common.verify.VerifyParamsUtil;
import cn.orangeiot.publish.model.ResultInfo;
import cn.orangeiot.publish.service.BaseService;
import cn.orangeiot.publish.service.GatewayDeviceService;
import cn.orangeiot.reg.gateway.GatewayAddr;
import cn.orangeiot.reg.memenet.MemenetAddr;
import cn.orangeiot.reg.message.MessageAddr;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import sun.misc.resources.Messages_es;

import java.util.Objects;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-01-04
 */
public class GatewayDeviceServiceImpl extends BaseService implements GatewayDeviceService {

    private static Logger logger = LogManager.getLogger(GatewayDeviceServiceImpl.class);

    private Vertx vertx;

    public GatewayDeviceServiceImpl(Vertx vertx, JsonObject jsonObject) {
        super(vertx, jsonObject);
        this.vertx = vertx;
    }


    /**
     * @Description 用户绑定网关
     * @author zhang bo
     * @date 18-1-4
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    @Override
    public void bindGatewayByUser(JsonObject jsonObject, Handler<AsyncResult<JsonObject>> handler) {
        //数据校验
        VerifyParamsUtil.verifyParams(jsonObject, new JsonObject().put("devuuid", DataType.STRING)
                .put("uid", DataType.STRING), as -> {
            if (as.failed()) {
                handler.handle(Future.succeededFuture(new JsonObject().put("code", ErrorType.RESULT_PARAMS_FAIL.getKey())
                        .put("msg", ErrorType.RESULT_PARAMS_FAIL.getValue())));
            } else {
                ResultInfo<Object> result = new ResultInfo<>();
                vertx.eventBus().send(GatewayAddr.class.getName() + BIND_GATEWAY_USER, as.result(),
                        SendOptions.getInstance(), rs -> {
                            if (Objects.nonNull(rs.result().body())) {
                                if (!rs.result().headers().isEmpty()) {//通知管理员
                                    result.setErrorMessage(Integer.parseInt(rs.result().headers().get("code")), rs.result().headers().get("msg")
                                            , jsonObject.getString("func"));

                                    //发送通知给gateway管理员
                                    vertx.eventBus().send(MessageAddr.class.getName() + NOTIFY_GATEWAY_USER_ADMIN,
                                            new JsonObject().put("devuuid", as.result().getString("devuuid"))
                                                    .put("uid", as.result().getString("uid"))
                                                    .put("func", jsonObject.getString("func")), SendOptions.getInstance());
                                } else {
                                    result.setData(rs.result().body()).setFunc(jsonObject.getString("func"));

                                    //同步绑定关系到第三方
                                    vertx.eventBus().send(MemenetAddr.class.getName() + BIND_DEVICE_USER,
                                            new JsonObject().put("uid", as.result().getString("uid"))
                                                    .put("devicesn", as.result().getString("devuuid")), SendOptions.getInstance());
                                }
                                handler.handle(Future.succeededFuture(JsonObject.mapFrom(result)));
                            } else {
                                result.setErrorMessage(ErrorType.BIND_GATEWAY_FAIL.getKey(), ErrorType.BIND_GATEWAY_FAIL.getValue()
                                        , jsonObject.getString("func"));
                                handler.handle(Future.succeededFuture(JsonObject.mapFrom(result)));
                            }
                        });
            }
        });
    }


    /**
     * @Description 审批普通用户绑定网关
     * @author zhang bo
     * @date 18-1-8
     * @version 1.0
     */
    @Override
    public void approvalBindGateway(JsonObject jsonObject, Handler<AsyncResult<JsonObject>> handler) {
        //数据校验
        VerifyParamsUtil.verifyParams(jsonObject, new JsonObject().put("devuuid", DataType.STRING)
                .put("requestuid", DataType.STRING).put("uid", DataType.STRING).put("type", DataType.INTEGER), as -> {
            if (as.failed()) {
                handler.handle(Future.succeededFuture(new JsonObject().put("code", ErrorType.RESULT_PARAMS_FAIL.getKey())
                        .put("msg", ErrorType.RESULT_PARAMS_FAIL.getValue())));
            } else {
                vertx.eventBus().send(GatewayAddr.class.getName() + APPROVAL_GATEWAY_BIND, as.result(), SendOptions.getInstance(), rs -> {
                    if (Objects.nonNull(rs.result().body())) {
                        //发送通知给gateway申请人
                        vertx.eventBus().send(MessageAddr.class.getName() + REPLY_GATEWAY_USER,
                                new JsonObject().put("devuuid", as.result().getString("devuuid"))
                                        .put("requestuid", as.result().getString("requestuid"))
                                        .put("uid", as.result().getString("uid"))
                                        .put("func", jsonObject.getString("func"))
                                        .put("type", as.result().getInteger("type")), SendOptions.getInstance());

                        handler.handle(Future.succeededFuture(JsonObject.mapFrom(
                                new ResultInfo<>().setData(new JsonObject()).setFunc(jsonObject.getString("func")))));

                        if (as.result().getInteger("type") == 2)
                            //同步绑定关系到第三方
                            vertx.eventBus().send(MemenetAddr.class.getName() + BIND_DEVICE_USER,
                                    new JsonObject().put("uid", as.result().getString("uid"))
                                            .put("devicesn", as.result().getString("devuuid")), SendOptions.getInstance());
                    } else {
                        handler.handle(Future.succeededFuture(JsonObject.mapFrom(
                                new ResultInfo<>().setErrorMessage(ErrorType.APPROVAL_FAIL.getKey(), ErrorType.APPROVAL_FAIL.getValue()
                                        , jsonObject.getString("func"))
                        )));
                    }
                });
            }
        });
    }


    /**
     * @Description 獲取网关绑定列表
     * @author zhang bo
     * @date 18-1-10
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void getGatewayBindList(JsonObject jsonObject, Handler<AsyncResult<JsonObject>> handler) {
        //数据校验
        VerifyParamsUtil.verifyParams(jsonObject, new JsonObject().put("uid", DataType.STRING), as -> {
            if (as.failed()) {
                handler.handle(Future.succeededFuture(new JsonObject().put("code", ErrorType.RESULT_PARAMS_FAIL.getKey())
                        .put("msg", ErrorType.RESULT_PARAMS_FAIL.getValue())));
            } else {
                vertx.eventBus().send(GatewayAddr.class.getName() + GET_GATEWAY_BIND_LIST, as.result(),
                        SendOptions.getInstance(), rs -> {
                            if (Objects.nonNull(rs.result())) {
                                handler.handle(Future.succeededFuture(JsonObject.mapFrom(
                                        new ResultInfo<>().setData(rs.result().body()).setFunc(jsonObject.getString("func"))
                                )));
                            } else {
                                handler.handle(Future.succeededFuture(JsonObject.mapFrom(
                                        new ResultInfo<>().setErrorMessage(ErrorType.GET_GATEWAY_BIND_FAIL.getKey(), ErrorType.GET_GATEWAY_BIND_FAIL.getValue()
                                                , jsonObject.getString("func")))));
                            }
                        });
            }
        });
    }


    /**
     * @Description 获取审批列表
     * @author zhang bo
     * @date 18-1-10
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void approvalList(JsonObject jsonObject, Handler<AsyncResult<JsonObject>> handler) {
        //数据校验
        VerifyParamsUtil.verifyParams(jsonObject, new JsonObject().put("uid", DataType.STRING), as -> {
            if (as.failed()) {
                handler.handle(Future.succeededFuture(new JsonObject().put("code", ErrorType.RESULT_PARAMS_FAIL.getKey())
                        .put("msg", ErrorType.RESULT_PARAMS_FAIL.getValue())));
            } else {
                vertx.eventBus().send(GatewayAddr.class.getName() + GET_GATEWAY_APPROVAL_LIST, as.result(),
                        SendOptions.getInstance(), rs -> {
                            if (Objects.nonNull(rs.result())) {
                                handler.handle(Future.succeededFuture(JsonObject.mapFrom(
                                        new ResultInfo<>().setData(rs.result().body()).setFunc(jsonObject.getString("func"))
                                )));
                            } else {
                                handler.handle(Future.succeededFuture(JsonObject.mapFrom(
                                        new ResultInfo<>().setErrorMessage(ErrorType.GET_GATEWAY_BIND_FAIL.getKey(), ErrorType.GET_GATEWAY_BIND_FAIL.getValue()
                                                , jsonObject.getString("func")))));
                            }
                        });
            }
        });
    }


    /**
     * @Description 取消解绑
     * @author zhang bo
     * @date 18-1-15
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    @Override
    public void unbindGateway(JsonObject jsonObject, Handler<AsyncResult<JsonObject>> handler) {
        //数据校验
        VerifyParamsUtil.verifyParams(jsonObject, new JsonObject().put("uid", DataType.STRING)
                .put("devuuid", DataType.STRING), as -> {
            if (as.failed()) {
                handler.handle(Future.succeededFuture(new JsonObject().put("code", ErrorType.RESULT_PARAMS_FAIL.getKey())
                        .put("msg", ErrorType.RESULT_PARAMS_FAIL.getValue())));
            } else {
                vertx.eventBus().send(GatewayAddr.class.getName() + UNBIND_GATEWAY, as.result(),
                        SendOptions.getInstance(), rs -> {
                            if (Objects.nonNull(rs.result())) {
                                handler.handle(Future.succeededFuture(JsonObject.mapFrom(
                                        new ResultInfo<>().setData(rs.result().body()).setFunc(jsonObject.getString("func"))
                                )));

                                //同步第三方信息
                                vertx.eventBus().send(MemenetAddr.class.getName() + RELIEVE_DEVICE_USER, as.result().put("mult", rs.result().headers().get("mult"))
                                        , SendOptions.getInstance());
                            } else {
                                handler.handle(Future.succeededFuture(JsonObject.mapFrom(
                                        new ResultInfo<>().setErrorMessage(ErrorType.GET_GATEWAY_BIND_FAIL.getKey(), ErrorType.GET_GATEWAY_BIND_FAIL.getValue()
                                                , jsonObject.getString("func")))));
                            }
                        });
            }
        });
    }


    /**
     * @Description 删除网关用户
     * @author zhang bo
     * @date 18-1-15
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    @Override
    public void delGatewayUser(JsonObject jsonObject, Handler<AsyncResult<JsonObject>> handler) {
        //数据校验
        VerifyParamsUtil.verifyParams(jsonObject, new JsonObject().put("uid", DataType.STRING)
                .put("devuuid", DataType.STRING).put("_id",DataType.STRING), as -> {
            if (as.failed()) {
                handler.handle(Future.succeededFuture(new JsonObject().put("code", ErrorType.RESULT_PARAMS_FAIL.getKey())
                        .put("msg", ErrorType.RESULT_PARAMS_FAIL.getValue())));
            } else {
                vertx.eventBus().send(GatewayAddr.class.getName() + DEL_GW_USER, as.result(),
                        SendOptions.getInstance(), (AsyncResult<Message<JsonObject>> rs) -> {
                            if (Objects.nonNull(rs.result())) {
                                handler.handle(Future.succeededFuture(JsonObject.mapFrom(
                                        new ResultInfo<>().setData(new JsonObject()).setFunc(jsonObject.getString("func"))
                                )));

                                //同步第三方信息
                                vertx.eventBus().send(MemenetAddr.class.getName() + DEL_DEVICE_USER, as.result()
                                                .put("userid", rs.result().body().getLong("userid"))
                                        , SendOptions.getInstance());
                            } else {
                                handler.handle(Future.succeededFuture(JsonObject.mapFrom(
                                        new ResultInfo<>().setErrorMessage(ErrorType.GET_GATEWAY_BIND_FAIL.getKey(), ErrorType.GET_GATEWAY_BIND_FAIL.getValue()
                                                , jsonObject.getString("func")))));
                            }
                        });
            }
        });
    }


    /**
     * @Description 获取网关普通用户集
     * @author zhang bo
     * @date 18-1-15
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    @Override
    public void getGatewayUserList(JsonObject jsonObject, Handler<AsyncResult<JsonObject>> handler) {
        //数据校验
        VerifyParamsUtil.verifyParams(jsonObject, new JsonObject().put("uid", DataType.STRING)
                .put("devuuid", DataType.STRING), as -> {
            if (as.failed()) {
                handler.handle(Future.succeededFuture(new JsonObject().put("code", ErrorType.RESULT_PARAMS_FAIL.getKey())
                        .put("msg", ErrorType.RESULT_PARAMS_FAIL.getValue())));
            } else {
                vertx.eventBus().send(GatewayAddr.class.getName() + GET_GW_USER_LIST, as.result(),
                        SendOptions.getInstance(), (AsyncResult<Message<JsonArray>> rs) -> {
                            if (Objects.nonNull(rs.result())) {
                                handler.handle(Future.succeededFuture(JsonObject.mapFrom(
                                        new ResultInfo<>().setData(rs.result().body()).setFunc(jsonObject.getString("func"))
                                )));
                            } else {
                                handler.handle(Future.succeededFuture(JsonObject.mapFrom(
                                        new ResultInfo<>().setErrorMessage(ErrorType.GET_GATEWAY_BIND_FAIL.getKey(), ErrorType.GET_GATEWAY_BIND_FAIL.getValue()
                                                , jsonObject.getString("func")))));
                            }
                        });
            }
        });
    }
}
