package cn.orangeiot.job.verifycode;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-12-18
 */
public class VerifyCodeJob implements Job {

    private static Logger logger = LoggerFactory.getLogger(VerifyCodeJob.class);

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        logger.info("==VerifyCodeJob=execute===");

        JsonObject jsonObject = (JsonObject) jobExecutionContext.getJobDetail().getJobDataMap().get("config");
        Vertx vertx = (Vertx) jobExecutionContext.getJobDetail().getJobDataMap().get("vertx");

        vertx.eventBus().send(jsonObject.getString("send_verifyCodeCron"),"");
    }
}
