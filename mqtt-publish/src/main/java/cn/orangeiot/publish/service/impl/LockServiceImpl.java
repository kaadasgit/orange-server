package cn.orangeiot.publish.service.impl;

import cn.orangeiot.publish.service.LockService;
import cn.orangeiot.reg.adminlock.AdminlockAddr;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-07-20
 */
public class LockServiceImpl implements LockService {

    private static Logger logger = LogManager.getLogger(LockServiceImpl.class);

    private Vertx vertx;

    private JsonObject conf;

    public LockServiceImpl(Vertx vertx, JsonObject conf) {
        this.vertx = vertx;
        this.conf = conf;
    }

    /**
     * @Description 通過網關开门
     * @author zhang bo
     * @date 18-7-20
     * @version 1.0
     */
    @Override
    public void openLock(JsonObject jsonObject) {
        vertx.eventBus().send(AdminlockAddr.class.getName() + OPEN_LOCK_BY_GATEWAY, jsonObject);
    }
}
