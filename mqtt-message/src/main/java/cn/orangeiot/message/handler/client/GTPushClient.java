package cn.orangeiot.message.handler.client;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;

public class GTPushClient {
    private static Logger logger = LogManager.getLogger(GTPushClient.class);
    public static WebClient webClient;

    /**
     * @Description 配置push服务
     * @author chason
     * @date 19-01-22
     * @version 1.0
     */
    public void loadConf(Vertx vertx) {
        InputStream pushIn = GTPushClient.class.getResourceAsStream("/gt_push.json");
        String pushConf = "";
        try {
            pushConf = IOUtils.toString(pushIn, "UTF-8");//获取配置
            if (!pushConf.equals("")) {
                JsonObject json = new JsonObject(pushConf);
                webClient = WebClient.create(vertx, new WebClientOptions()
                        .setDefaultHost(json.getString("host"))
                        .setVerifyHost(json.getBoolean("verifyHost"))
                        .setTrustAll(json.getBoolean("trustAll"))
                        .setMaxPoolSize(json.getInteger("maxPoolSize")).setConnectTimeout(json.getInteger("timeout")));
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            if (null != pushIn)
                try {
                    pushIn.close();
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
        }
    }
}
