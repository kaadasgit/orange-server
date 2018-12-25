package cn.orangeiot.http.constant;


import cn.orangeiot.common.utils.DataType;
import cn.orangeiot.common.verify.VerifyParamsUtil;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-08-28
 */
public class UserRequestRateLimitConf {

    private static Logger logger = LogManager.getLogger(UserRequestRateLimitConf.class);

    private static int times;//限制次數

    private static long windowTime;//时间窗口

    private static Vertx vertx;

    public static int getTimes() {
        return times;
    }

    public static Vertx getVertx() {
        return vertx;
    }

    public static long getWindowTime() {
        return windowTime;
    }

    public UserRequestRateLimitConf(Vertx vertx) {
        this.vertx = vertx;
    }

    /**
     * @Description 加載限流配置
     * @author zhang bo
     * @date 18-8-27
     * @version 1.0
     */
    public void loadRateLimitConf() {
        ConfigStoreOptions fileStore = new ConfigStoreOptions()
                .setType("file")
                .setConfig(new JsonObject().put("path", System.getProperty("RATELIMITPATH")));

        ConfigRetrieverOptions options = new ConfigRetrieverOptions()
                .addStore(fileStore);

        ConfigRetriever retriever = ConfigRetriever.create(vertx, options);
        RateLimitProcess(retriever);
    }


    /**
     * @Description 限流设置处理
     * @author zhang bo
     * @date 18-8-27
     * @version 1.0
     */
    public void RateLimitProcess(ConfigRetriever retriever) {
        //获取配置
        retriever.getConfig(result -> {
            if (result.failed()) {
                logger.fatal("ratelimit config path no exists");
                System.exit(1);
            } else
                VerifyParamsUtil.verifyParams(result.result(), new JsonObject().put("times", DataType.INTEGER)
                        .put("windowTime", DataType.INTEGER), rs -> {
                    if (rs.failed()) {
                        logger.fatal("ratelimit config fail");
                        System.exit(1);
                    } else {
                        times = rs.result().getInteger("times");
                        windowTime = rs.result().getLong("windowTime");
                    }
                });
        });
        //listen changed conf
        retriever.listen(change -> {
            JsonObject conf = change.getNewConfiguration();
            VerifyParamsUtil.verifyParams(conf, new JsonObject().put("windowTime", DataType.INTEGER).put("times", DataType.INTEGER), rs -> {
                if (rs.failed()) {
                    logger.fatal("ratelimit config listen changed fail");
                } else {
                    times = rs.result().getInteger("times");
                    windowTime = rs.result().getLong("windowTime");
                }
            });
        });
    }
}
