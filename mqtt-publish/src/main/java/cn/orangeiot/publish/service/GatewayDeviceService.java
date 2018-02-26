package cn.orangeiot.publish.service;

import cn.orangeiot.reg.gateway.GatewayAddr;
import cn.orangeiot.reg.memenet.MemenetAddr;
import cn.orangeiot.reg.message.MessageAddr;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-01-04
 */
public interface GatewayDeviceService extends GatewayAddr,MessageAddr,MemenetAddr{

    void bindGatewayByUser(JsonObject jsonObject, Handler<AsyncResult<JsonObject>> handler);

    void approvalBindGateway(JsonObject jsonObject, Handler<AsyncResult<JsonObject>> handler);

    void getGatewayBindList(JsonObject jsonObject, Handler<AsyncResult<JsonObject>> handler);

    void approvalList(JsonObject jsonObject, Handler<AsyncResult<JsonObject>> handler);

    void unbindGateway(JsonObject jsonObject, Handler<AsyncResult<JsonObject>> handler);

    void delGatewayUser(JsonObject jsonObject, Handler<AsyncResult<JsonObject>> handler);

    void getGatewayUserList(JsonObject jsonObject, Handler<AsyncResult<JsonObject>> handler);
}
