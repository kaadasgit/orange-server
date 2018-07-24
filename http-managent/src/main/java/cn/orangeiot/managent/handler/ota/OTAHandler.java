package cn.orangeiot.managent.handler.ota;

import cn.orangeiot.common.genera.ErrorType;
import cn.orangeiot.common.genera.Result;
import cn.orangeiot.common.options.SendOptions;
import cn.orangeiot.common.utils.CreateOTAOrderNoUtils;
import cn.orangeiot.common.utils.DataType;
import cn.orangeiot.common.utils.UUIDUtils;
import cn.orangeiot.managent.collector.DateRangeCollector;
import cn.orangeiot.managent.collector.ModelCollector;
import cn.orangeiot.managent.verify.VerifyParamsUtil;
import cn.orangeiot.reg.EventbusAddr;
import cn.orangeiot.reg.ota.OtaAddr;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Objects;
import java.util.UUID;

/**
 * @author zhang bo
 * @version 1.0
 * @Description OTA升級管理
 * @date 2018-03-28
 */
public class OTAHandler implements EventbusAddr {

    private static Logger logger = LogManager.getLogger(OTAHandler.class);

    private Vertx vertx;

    private JsonObject conf;

    public OTAHandler(Vertx vertx, JsonObject conf) {
        this.vertx = vertx;
        this.conf = conf;
    }


    /**
     * @Description 查詢所有產品
     * @author zhang bo
     * @date 18-3-28
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void selectModelAll(RoutingContext routingContext) {
        logger.info("===OTAHandler==selectRange==params -> {}", routingContext.getBodyAsString());

        vertx.eventBus().send(OtaAddr.class.getName() + SELECT_MODEL, "", SendOptions.getInstance()
                , (AsyncResult<Message<JsonObject>> rs) -> {
                    if (rs.failed()) {
                        rs.cause().printStackTrace();
                        routingContext.fail(501);
                    } else {
                        if (Objects.nonNull(rs.result().body().getValue("result"))) {
                            JsonArray jsonArray = rs.result().body().getJsonArray("result")
                                    .stream().map(e -> {
                                        JsonObject dataObject = new JsonObject(e.toString());
                                        JsonObject resultObject = new JsonObject().put("modelCode", dataObject.getJsonObject("_id").getString("modelCode"))
                                                .put("childCode", dataObject.getJsonObject("_id").getString("childCode"));
                                        return resultObject;
                                    }).collect(new ModelCollector());
                            routingContext.response().end(JsonObject.mapFrom(new Result<JsonArray>()
                                    .setData(jsonArray)).toString());
                        } else {
                            routingContext.response().end(JsonObject.mapFrom(new Result<JsonArray>()).toString());
                        }
                    }
                });
    }


    /**
     * @Description 查询時間范围
     * @author zhang bo
     * @date 18-3-30
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void selectDateRange(RoutingContext routingContext) {
        logger.info("===OTAHandler==selectDateRange==params -> {}", routingContext.getBodyAsString());

        VerifyParamsUtil.verifyParams(routingContext, new JsonObject().put("modelCode", DataType.STRING)
                .put("childCode", DataType.STRING), rs -> {
            if (rs.failed()) {
                routingContext.fail(401);
            } else {
                vertx.eventBus().send(OtaAddr.class.getName() + SELECT_DATE_RANGE, rs.result(), SendOptions.getInstance()
                        , (AsyncResult<Message<JsonArray>> as) -> {
                            if (as.failed()) {
                                as.cause().printStackTrace();
                                routingContext.fail(501);
                            } else {
                                if (Objects.nonNull(as.result().body())) {
                                    JsonArray jsonArray = as.result().body().stream().map(e -> new JsonObject(e.toString()))
                                            .collect(new DateRangeCollector());
                                    routingContext.response().end(JsonObject.mapFrom(new Result<JsonArray>()
                                            .setData(jsonArray)).toString());
                                } else {
                                    routingContext.response().end(JsonObject.mapFrom(new Result<JsonArray>()).toString());
                                }
                            }
                        });
            }
        });
    }


    /**
     * @Description 查询編號範圍
     * @author zhang bo
     * @date 18-3-30
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void selectNumRange(RoutingContext routingContext) {
        logger.info("===OTAHandler==selectNORange==params -> {}", routingContext.getBodyAsString());

        VerifyParamsUtil.verifyParams(routingContext, new JsonObject().put("modelCode", DataType.STRING)
                .put("childCode", DataType.STRING).put("weekCode", DataType.STRING).put("yearCode", DataType.STRING), rs -> {
            if (rs.failed()) {
                routingContext.fail(401);
            } else {
                vertx.eventBus().send(OtaAddr.class.getName() + SELECT_NUM_RANGE, rs.result(), SendOptions.getInstance()
                        , (AsyncResult<Message<Integer>> as) -> {
                            if (as.failed()) {
                                as.cause().printStackTrace();
                                routingContext.fail(501);
                            } else {
                                routingContext.response().end(JsonObject.mapFrom(new Result<Integer>()
                                        .setData(as.result().body())).toString());
                            }
                        });
            }
        });
    }

    /**
     * @Description 提交ota升級
     * @author zhang bo
     * @date 18-4-2
     * @version 1.0
     */
    public void submitOTAUpgrade(RoutingContext routingContext) {
        logger.info("===OTAHandler==submitOTAUpgrade==params -> {}", routingContext.getBodyAsString());

        VerifyParamsUtil.verifyParams(routingContext, new JsonObject().put("modelCode", DataType.STRING)
                .put("childCode", DataType.STRING).put("yearCode", DataType.STRING).put("weekCode", DataType.STRING)
                .put("type", DataType.INTEGER).put("range", DataType.STRING).put("filePathUrl", DataType.STRING)
                .put("modelType", DataType.INTEGER).put("SW", DataType.STRING)
                .put("fileMd5", DataType.STRING).put("fileLen", DataType.INTEGER), rs -> {
            if (rs.failed()) {
                routingContext.fail(401);
            } else {
                if (rs.result().getString("range").indexOf("-") > 0
                        || rs.result().getString("range").indexOf(",") > 0) {

                    String orderNO = CreateOTAOrderNoUtils.getOTAOrderNo();//生成ota唯一ID单号
                    rs.result().put("OTAOrderNo", orderNO);

                    routingContext.response().end(JsonObject.mapFrom(new Result<JsonObject>()
                            .setData(new JsonObject().put("OTAOrderNo", orderNO))).toString());
                    //保存記錄
                    vertx.eventBus().send(OtaAddr.class.getName() + SUBMIT_OTA_UPGRADE, rs.result());
                    //升级处理
                    vertx.eventBus().send(OtaAddr.class.getName() + OTA_UPGRADE_PROCESS, rs.result());
                } else
                    routingContext.response().end(JsonObject.mapFrom(new Result<>()
                            .setErrorMessage(ErrorType.RESULT_DATA_FAIL.getKey(), ErrorType.RESULT_DATA_FAIL.getValue())).toString());
            }
        });
    }
}
