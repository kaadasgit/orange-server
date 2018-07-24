package cn.orangeiot.publish.service;

import cn.orangeiot.reg.adminlock.AdminlockAddr;
import io.vertx.core.json.JsonObject;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-07-20
 */
public interface LockService extends AdminlockAddr {

    void openLock(JsonObject jsonObject);
}
