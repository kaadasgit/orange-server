package cn.orangeiot.apidao.handler.dao.test;

import cn.orangeiot.apidao.client.MongoClient;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.UpdateOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-12-25
 */
public class TestProcessDao {

    private static Logger logger = LogManager.getLogger(TestProcessDao.class);

    private Vertx vertx;

    public TestProcessDao(Vertx vertx) {
        this.vertx = vertx;
    }


    /**
     * @Description 測試綁定網關
     * @author zhang bo
     * @date 18-12-25
     * @version 1.0
     */
    public void testBindGateway(Message<JsonObject> message) {
    }


    /**
     * @Description 測試解綁網關
     * @author zhang bo
     * @date 18-12-25
     * @version 1.0
     */
    public void testUnBindGateway(Message<JsonObject> message) {
        MongoClient.client.updateCollectionWithOptions("kdsGatewayDeviceList", new JsonObject().put("deviceSN", message.body().getString("devuuid"))
                .put("adminuid", message.body().getString("uid")), new JsonObject().put("$unset", new JsonObject()
                .put("username", "").put("userNickname", "").put("adminuid", "").put("adminName", "").put("adminNickname", "")
                .put("isAdmin", "").put("bindTime", "").put("uid", "")), new UpdateOptions().setMulti(false), res -> {
            if (res.failed()) {
                logger.error(res.cause().getMessage(), res);
                message.reply(null);
            } else {
                message.reply(new JsonObject());
            }
        });
    }


}
