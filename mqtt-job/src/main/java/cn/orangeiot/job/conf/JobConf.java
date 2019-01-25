package cn.orangeiot.job.conf;

import cn.orangeiot.job.JobStart;
import cn.orangeiot.job.verifycode.GtAuthtokenJob;
import cn.orangeiot.job.verifycode.VerifyCodeJob;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.*;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-12-18
 */
public class JobConf {

    private Vertx vertx;

    private JsonObject configJson;

    private Scheduler scheduler;

    private static Logger logger = LogManager.getLogger(JobConf.class);

    public JobConf(Vertx vertx, JsonObject configJson, Scheduler scheduler) {
        this.vertx = vertx;
        this.configJson = configJson;
        this.scheduler = scheduler;
    }

    /**
     * @Description 0点重置短信发送次数
     * @author zhang bo
     * @date 17-12-18
     * @version 1.0
     */
    public void verifyCode() {
        //创建job任务
        JobDetail jobDetail = newJob(VerifyCodeJob.class)
                .withIdentity("VerifyCodeJob", "DEFAULT")
                .usingJobData(new JobDataMap() {{
                    put("vertx", vertx);
                    put("config", configJson);
                }}).build();
        //2、创建Trigger
        Trigger trigger = newTrigger().withIdentity(
                "VerifyCodeTrigger", "DEFAULT")
                .forJob("VerifyCodeJob", "DEFAULT").startNow().withSchedule(
                        CronScheduleBuilder.cronSchedule(configJson.getString("verifyCodeCron"))).build();
        //4、调度器和job关联


        // 个推job任务
        JobDetail gtJob = newJob(GtAuthtokenJob.class)
                .withIdentity("GtAuthtokenJob", "DEFAULT")
                .usingJobData(new JobDataMap() {{
                    put("vertx", vertx);
                    put("config", configJson);
                }}).build();
        Trigger gtTrigger = newTrigger().withIdentity(
                "GtAuthtokenTrigger", "DEFAULT")
                .forJob("GtAuthtokenJob", "DEFAULT").withSchedule(
                        SimpleScheduleBuilder.simpleSchedule().withIntervalInHours(12).repeatForever()).build();
        try {
            scheduler.scheduleJob(jobDetail, trigger);
            scheduler.scheduleJob(gtJob, gtTrigger);
        } catch (SchedulerException e) {
            logger.error(e.getMessage(), e);
        }
    }

}
