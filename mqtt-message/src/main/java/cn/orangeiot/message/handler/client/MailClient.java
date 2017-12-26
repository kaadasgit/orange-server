package cn.orangeiot.message.handler.client;

import io.vertx.core.Vertx;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.StartTLSOptions;

import java.io.InputStream;
import java.util.Properties;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-12-12
 */
public class MailClient {

    public static io.vertx.ext.mail.MailClient client;

    /**r
     * @Description 配置邮箱服务
     * @author zhang bo
     * @date 17-11-23
     * @version 1.0
     */
    public void mailConf(Vertx vertx){
        InputStream mainIn = MailClient.class.getResourceAsStream("/email-config.properties");
        try {
            Properties mainConfig = new Properties();
            mainConfig.load(mainIn);

            MailConfig config = new MailConfig();
            config.setHostname(mainConfig.getProperty("mailServerHost"));
            config.setPort(Integer.parseInt(mainConfig.getProperty("mailServerPort")));
            config.setStarttls(StartTLSOptions.DISABLED);
            config.setUsername(mainConfig.getProperty("userName"));
            config.setPassword(mainConfig.getProperty("password"));
            config.setMaxPoolSize(Integer.parseInt(mainConfig.getProperty("maxPool")));
            client = io.vertx.ext.mail.MailClient.createShared(vertx, config);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
