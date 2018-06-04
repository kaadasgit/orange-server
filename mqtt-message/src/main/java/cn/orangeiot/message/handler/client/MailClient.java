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

    public static io.vertx.ext.mail.MailClient kaadasClient;

    public static io.vertx.ext.mail.MailClient philipClient;

    /**
     * r
     *
     * @Description 配置邮箱服务
     * @author zhang bo
     * @date 17-11-23
     * @version 1.0
     */
    public void mailConf(Vertx vertx) {
        InputStream mainIn = MailClient.class.getResourceAsStream("/email-config.properties");
        try {
            Properties mainConfig = new Properties();
            mainConfig.load(mainIn);

            MailConfig kaadasConfig = new MailConfig();
            kaadasConfig.setHostname(mainConfig.getProperty("kaadasmailServerHost"));
            kaadasConfig.setPort(Integer.parseInt(mainConfig.getProperty("kaadasmailServerPort")));
            kaadasConfig.setStarttls(StartTLSOptions.DISABLED);
            kaadasConfig.setUsername(mainConfig.getProperty("kaadasuserName"));
            kaadasConfig.setPassword(mainConfig.getProperty("kaadaspassword"));
            kaadasConfig.setMaxPoolSize(Integer.parseInt(mainConfig.getProperty("maxPool")));
            kaadasClient = io.vertx.ext.mail.MailClient.createNonShared(vertx, kaadasConfig);

            MailConfig philipConfig = new MailConfig();
            philipConfig.setHostname(mainConfig.getProperty("philipmailServerHost"));
            philipConfig.setPort(Integer.parseInt(mainConfig.getProperty("philipmailServerPort")));
            philipConfig.setStarttls(StartTLSOptions.DISABLED);
            philipConfig.setUsername(mainConfig.getProperty("philipuserName"));
            philipConfig.setPassword(mainConfig.getProperty("philippassword"));
            philipConfig.setMaxPoolSize(Integer.parseInt(mainConfig.getProperty("maxPool")));
            philipClient = io.vertx.ext.mail.MailClient.createNonShared(vertx, philipConfig);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
