package cn.orangeiot.publish.service.impl;

import cn.orangeiot.common.genera.ErrorType;
import cn.orangeiot.common.utils.DataType;
import cn.orangeiot.common.verify.VerifyParamsUtil;
import cn.orangeiot.publish.model.ResultInfo;
import cn.orangeiot.publish.service.TestProcessService;
import cn.orangeiot.reg.testservice.TestProcessAddr;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;

import java.util.Objects;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-12-25
 */
public class TestProcessServiceImpl implements TestProcessService {


    private EventBus eb;

    public TestProcessServiceImpl(EventBus eb) {
        this.eb = eb;
    }

    @Override
    public void testBindGateway(JsonObject jsonObject, Handler<AsyncResult<JsonObject>> handler) {
        VerifyParamsUtil.verifyParams(jsonObject, new JsonObject().put("uid", DataType.STRING)
                .put("devuuid", DataType.STRING), res -> {
            if (res.failed()) {
                handler.handle(Future.succeededFuture(new JsonObject().put("code", ErrorType.RESULT_PARAMS_FAIL.getKey())
                        .put("msg", ErrorType.RESULT_PARAMS_FAIL.getValue())));
            } else {
                eb.send(TestProcessAddr.class.getName() + TestProcessAddr.TEST_BIND_GATEWAY, res.result(), rs -> {
                    if (rs.failed()) {
                        handler.handle(Future.succeededFuture(JsonObject.mapFrom(
                                new ResultInfo<>().setErrorMessage(ErrorType.UNBIND_GATEWAY_BIND_FAIL.getKey(), ErrorType.UNBIND_GATEWAY_BIND_FAIL.getValue()
                                        , jsonObject.getString("func")))));
                    } else {

                    }
                });
            }
        });
    }


    @Override
    public void testUnBindGateway(JsonObject jsonObject, Handler<AsyncResult<JsonObject>> handler) {
        VerifyParamsUtil.verifyParams(jsonObject, new JsonObject().put("uid", DataType.STRING)
                .put("devuuid", DataType.STRING), res -> {
            if (res.failed()) {
                handler.handle(Future.succeededFuture(new JsonObject().put("code", ErrorType.RESULT_PARAMS_FAIL.getKey())
                        .put("msg", ErrorType.RESULT_PARAMS_FAIL.getValue())));
            } else {
                eb.send(TestProcessAddr.class.getName() + TEST_UIBIND_GATEWAY, res.result(), rs -> {
                    if (rs.failed()) {
                        handler.handle(Future.succeededFuture(JsonObject.mapFrom(
                                new ResultInfo<>().setErrorMessage(ErrorType.UNBIND_GATEWAY_BIND_FAIL.getKey(), ErrorType.UNBIND_GATEWAY_BIND_FAIL.getValue()
                                        , jsonObject.getString("func")))));
                    } else {
                        if (Objects.nonNull(rs.result()) && Objects.nonNull(rs.result().body())) {
                            handler.handle(Future.succeededFuture(JsonObject.mapFrom(
                                    new ResultInfo<>().setData(rs.result().body()).setFunc(jsonObject.getString("func"))
                            )));
                        } else {
                            handler.handle(Future.succeededFuture(JsonObject.mapFrom(
                                    new ResultInfo<>().setErrorMessage(ErrorType.UNBIND_GATEWAY_BIND_FAIL.getKey(), ErrorType.UNBIND_GATEWAY_BIND_FAIL.getValue()
                                            , jsonObject.getString("func")))));
                        }
                    }
                });
            }
        });
    }
}
