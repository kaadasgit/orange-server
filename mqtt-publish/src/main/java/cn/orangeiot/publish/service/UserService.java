package cn.orangeiot.publish.service;

import cn.orangeiot.reg.EventbusAddr;
import cn.orangeiot.reg.user.UserAddr;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-05-04
 */
public interface UserService extends EventbusAddr{
    void selectGWAdmin(JsonObject jsonObject);
}
