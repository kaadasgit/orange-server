package cn.orangeiot.publish.service;

import cn.orangeiot.reg.testservice.TestProcessAddr;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-12-24
 */
public interface TestProcessService  extends TestProcessAddr{


    /**
     * @Description 測試賬戶綁定設備
     * @author zhang bo
     * @date 18-12-20
     * @version 1.0
     * @param jsonObject 数据
     */
    void testBindGateway(JsonObject jsonObject, Handler<AsyncResult<JsonObject>> handler);

    /**
     * @Description 測試賬戶解綁設備
     * @author zhang bo
     * @date 18-12-20
     * @version 1.0
     * @param jsonObject 数据
     */
    void testUnBindGateway(JsonObject jsonObject,Handler<AsyncResult<JsonObject>> handler);
}
