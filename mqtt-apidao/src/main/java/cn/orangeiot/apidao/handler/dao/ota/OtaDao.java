package cn.orangeiot.apidao.handler.dao.ota;

import cn.orangeiot.apidao.client.MongoClient;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Objects;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-03-29
 */
public class OtaDao {

    private static Logger logger = LogManager.getLogger(OtaDao.class);


    /**
     * @Description 查詢產品類型
     * @author zhang bo
     * @date 18-3-29
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void selectModelType(Message<String> message) {
        logger.info("==selectModelType==params -> {}", message.body());

        MongoClient.client.runCommand("aggregate", new JsonObject().put("aggregate", "kdsModelInfo")
                .put("pipeline", new JsonArray().add(new JsonObject().put("$group", new JsonObject().put("_id"
                        , new JsonObject().put("modelCode", "$modelCode").put("childCode", "$childCode"))))), rs -> {
            if (rs.failed()) {
                rs.cause().printStackTrace();
            } else {
                logger.info("selectModelType==mongo== result -> {}", rs.result());
                message.reply(rs.result());
            }
        });
    }


    /**
     * @Description 查詢時期範圍
     * @author zhang bo
     * @date 18-3-29
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void selectDateRange(Message<JsonObject> message) {
        logger.info("==selectDateRange==params -> {}", message.body());

        MongoClient.client.findWithOptions("kdsModelInfo", new JsonObject().put("modelCode", message.body().getString("modelCode"))
                        .put("childCode", message.body().getString("childCode"))
                , new FindOptions().setFields(new JsonObject().put("yearCode", 1).put("weekCode", 1).put("_id", 0)), rs -> {
                    if (rs.failed()) {
                        rs.cause().printStackTrace();
                    } else {
                        logger.info("selectModelType==mongo== result -> {}", rs.result());
                        message.reply(new JsonArray(rs.result()));
                    }
                });
    }


    /**
     * @Description 查询編號範圍
     * @author zhang bo
     * @date 18-3-29
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void selectNumRange(Message<JsonObject> message) {
        logger.info("==selectNumRange==params -> {}", message.body());

        MongoClient.client.findWithOptions("kdsModelInfo", new JsonObject().put("modelCode", message.body().getString("modelCode"))
                        .put("childCode", message.body().getString("childCode")).put("yearCode", message.body().getString("yearCode"))
                        .put("weekCode", message.body().getString("weekCode"))
                , new FindOptions().setFields(new JsonObject().put("count", 1).put("_id", 0)), rs -> {
                    if (rs.failed()) {
                        rs.cause().printStackTrace();
                    } else {
                        logger.info("selectModelType==mongo== result -> {}", rs.result());
                        int sum = 0;
                        if (rs.result().size() > 0) {
                            sum = rs.result().stream().mapToInt(e -> new JsonObject(e.toString()).getInteger("count")).sum();
                        }
                        message.reply(sum);
                    }
                });
    }


    /**
     * @Description 提交ota升级的数据
     * @author zhang bo
     * @date 18-4-2
     * @version 1.0
     */
    public void submitOTAUpgrade(Message<JsonObject> message) {
        logger.info("==submitOTAUpgrade==params -> {}", message.body());

        MongoClient.client.insert("kdsOtaUpgrade", message.body(), rs -> {
            if (rs.failed()) rs.cause().printStackTrace();
        });


    }
}
