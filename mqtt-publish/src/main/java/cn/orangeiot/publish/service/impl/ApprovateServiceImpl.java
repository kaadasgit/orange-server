package cn.orangeiot.publish.service.impl;

import cn.orangeiot.common.genera.ErrorType;
import cn.orangeiot.common.utils.DataType;
import cn.orangeiot.common.utils.UUIDUtils;
import cn.orangeiot.common.verify.VerifyParamsUtil;
import cn.orangeiot.publish.service.ApprovateService;
import cn.orangeiot.publish.service.BaseService;
import cn.orangeiot.reg.ota.OtaAddr;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-04-27
 */
public class ApprovateServiceImpl extends BaseService implements ApprovateService {

    private static Logger logger = LogManager.getLogger(ApprovateServiceImpl.class);

    private Vertx vertx;

    private JsonObject conf;

    public ApprovateServiceImpl(Vertx vertx, JsonObject conf) {
        super(vertx, conf);
        this.vertx = vertx;
        this.conf = conf;
    }

    /**
     * @Description ota升级
     * @author zhang bo
     * @date 18-4-27
     * @version 1.0
     */
    @Override
    public void approvateOTA(JsonObject jsonObject, Handler<AsyncResult<JsonObject>> handler) {
        logger.debug("params -> {}", jsonObject);
        //数据校验
        if (Objects.nonNull(jsonObject.getValue("userId")) && Objects.nonNull(jsonObject.getValue("params")) &&
                Objects.nonNull(jsonObject.getJsonObject("params").getValue("type"))
                && Objects.nonNull(jsonObject.getValue("OTAOrderNo"))) {
            if (jsonObject.getJsonObject("params").getInteger("type") == 1) {//同意升级
                logger.info("OTA Approvate ok ,userId -> {}", jsonObject.getString("userId"));
                JsonObject result = jsonObject.put("func",
                        conf.getString("OTA_Notify"));
                handler.handle(Future.succeededFuture(result));
            } else {
                logger.info("OTA Approvate fail ,userId -> {}", jsonObject.getString("userId"));
                handler.handle(Future.failedFuture("User Approvate fail"));
            }
            //记录OTA审批日志
            vertx.eventBus().send(OtaAddr.class.getName() + OTA_APPROVATE_RECORD, new JsonObject()
                    .put("uid", jsonObject.getString("userId")).put("deviceList", jsonObject.getJsonObject("params")
                            .getJsonArray("deviceList"))
                    .put("type", jsonObject.getJsonObject("params").getInteger("type"))
                    .put("fileUrl", jsonObject.getJsonObject("params").getString("fileUrl"))
                    .put("SW", jsonObject.getJsonObject("params").getString("SW"))
                    .put("time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .put("OTAOrderNo", jsonObject.getString("OTAOrderNo")));
        } else {
            handler.handle(Future.failedFuture("Approvate OTA params verify fail"));
        }
    }
}
