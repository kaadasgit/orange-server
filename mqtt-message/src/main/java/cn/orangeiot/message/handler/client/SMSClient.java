package cn.orangeiot.message.handler.client;

import cn.orangeiot.message.handler.RegisterHandler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-01-24
 */
public class SMSClient {
    public static io.vertx.ext.web.client.WebClient client;

    private static Logger logger = LogManager.getLogger(SMSClient.class);
    /**
     * @Description 配置邮箱服务
     * @author zhang bo
     * @date 17-11-23
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void smsConf(Vertx vertx) {
        InputStream mongoIn = SMSClient.class.getResourceAsStream("/sms-conf.json");
        String httpConf = "";//jdbc连接配置
        try {
            httpConf = IOUtils.toString(mongoIn, "UTF-8");//获取配置

            if (!httpConf.equals("")) {
                JsonObject json = new JsonObject(httpConf);
                client = io.vertx.ext.web.client.WebClient.create(vertx, new WebClientOptions()
                        .setDefaultHost(json.getString("host")).setDefaultPort(json.getInteger("port"))
                        .setVerifyHost(json.getBoolean("verifyHost")).setSsl(json.getBoolean("is_ssl"))
                        .setTrustAll(json.getBoolean("trustAll"))
                        .setMaxPoolSize(json.getInteger("maxPoolSize")).setConnectTimeout(json.getInteger("timeout")));
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            if (null != mongoIn)
                try {
                    mongoIn.close();
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
        }
    }
}
