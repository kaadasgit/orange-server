package cn.orangeiot.publish.service;

import cn.orangeiot.reg.adminlock.AdminlockAddr;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-01-04
 */
public interface AdminLockService extends AdminlockAddr {

    void createAdminDev(JsonObject jsonObject, Handler<AsyncResult<JsonObject>> handler);

    void deletevendorDev(JsonObject jsonObject, Handler<AsyncResult<JsonObject>> handler);

    void deleteAdminDev(JsonObject jsonObject, Handler<AsyncResult<JsonObject>> handler);

    void deleteNormalDev(JsonObject jsonObject, Handler<AsyncResult<JsonObject>> handler);

    void createNormalDev(JsonObject jsonObject, Handler<AsyncResult<JsonObject>> handler);

    void downloadOpenLocklist(JsonObject jsonObject, Handler<AsyncResult<JsonObject>> handler);

    void updateNormalDevlock(JsonObject jsonObject, Handler<AsyncResult<JsonObject>> handler);

    void adminOpenLock(JsonObject jsonObject, Handler<AsyncResult<JsonObject>> handler);

    void getAdminDevlist(JsonObject jsonObject, Handler<AsyncResult<JsonObject>> handler);

    void getNormalDevlist(JsonObject jsonObject, Handler<AsyncResult<JsonObject>> handler);

    void editAdminDev(JsonObject jsonObject, Handler<AsyncResult<JsonObject>> handler);

    void getAdminDevlocklongtitude(JsonObject jsonObject, Handler<AsyncResult<JsonObject>> handler);

    void updateAdminDevAutolock(JsonObject jsonObject, Handler<AsyncResult<JsonObject>> handler);

    void updateAdminlockNickName(JsonObject jsonObject, Handler<AsyncResult<JsonObject>> handler);

    void checkAdmindev(JsonObject jsonObject, Handler<AsyncResult<JsonObject>> handler);

    void uploadOpenLockList(JsonObject jsonObject, Handler<AsyncResult<JsonObject>> handler);
}
