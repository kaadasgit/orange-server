package cn.orangeiot.publish.service;

import cn.orangeiot.reg.ota.OtaAddr;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-04-27
 */
public interface ApprovateService extends OtaAddr {

    void approvateOTA(JsonObject jsonObject, Handler<AsyncResult<JsonObject>> handler);
}
