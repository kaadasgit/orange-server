package cn.orangeiot.job.verifycode;

import cn.orangeiot.common.utils.SHA256;
import cn.orangeiot.job.client.GTPushClient;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class GtAuthtokenJob implements Job {
    private static Logger logger = LogManager.getLogger(GtAuthtokenJob.class);

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        logger.info("==GtAuthtokenJob=execute===");

        JsonObject jsonObject = (JsonObject) jobExecutionContext.getJobDetail().getJobDataMap().get("config");
        Vertx vertx = (Vertx) jobExecutionContext.getJobDetail().getJobDataMap().get("vertx");

        String appKey = jsonObject.getString("gt_appKey");
        String timeStampStr = String.valueOf(System.currentTimeMillis());
        String mastersecret = jsonObject.getString("gt_masterSecret");
        SHA256.getSHA256Str((appKey + timeStampStr + mastersecret), rs -> {
            String sign = rs.result();

            JsonObject params = new JsonObject().put("sign", sign).put("timestamp", timeStampStr).put("appkey", appKey);
            GTPushClient gtPushClient = new GTPushClient();
            gtPushClient.loadConf(vertx);
            GTPushClient.webClient.post("/v1/5zODhkZOQd66zCoOgxX152/auth_sign")
                    .putHeader("content-type", "application/json")
                    .sendJsonObject(params, res -> {
                        if (res.failed()) {
                            logger.error(rs.cause().getMessage(), rs);
                        } else {
                            logger.info("request result url /v1/5zODhkZOQd66zCoOgxX152/auth_sign  , result -> {}",
                                    res.result().body().toString());
                            JsonObject resultJson = new JsonObject(res.result().body().toString());
                            JsonObject rParams = new JsonObject().put("appId", jsonObject.getString("gt_appId")).put("auth_token", resultJson.getString("auth_token"));
                            if (StringUtils.isNotBlank(resultJson.getString("result")) && "ok".equals(resultJson.getString("result"))) {
                                vertx.eventBus().send(jsonObject.getString("send_gtAuthtokenCron"),rParams);
                            }
                        }
                    });
        });
    }
}
