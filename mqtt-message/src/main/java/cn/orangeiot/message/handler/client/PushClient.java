package cn.orangeiot.message.handler.client;

import cn.orangeiot.message.constant.ConstantConf;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.PfxOptions;
import io.vertx.ext.web.client.WebClientOptions;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-05-21
 */
public class PushClient {

    public static io.vertx.ext.web.client.WebClient androidClient;

    public static io.vertx.ext.web.client.WebClient iosClient;

    /**
     * @Description 配置androdi push服务
     * @author zhang bo
     * @date 17-11-23
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void loadAndroidConf(Vertx vertx) {
        InputStream pushIn = SMSClient.class.getResourceAsStream("/push.json");
        String pushConf = "";//jdbc连接配置
        try {
            pushConf = IOUtils.toString(pushIn, "UTF-8");//获取配置

            if (!pushConf.equals("")) {
                JsonObject json = new JsonObject(pushConf);
                androidClient = io.vertx.ext.web.client.WebClient.create(vertx, new WebClientOptions()
                        .setDefaultHost(json.getString("host")).setDefaultPort(json.getInteger("port"))
                        .setVerifyHost(json.getBoolean("verifyHost")).setSsl(json.getBoolean("is_ssl"))
                        .setTrustAll(json.getBoolean("trustAll"))
                        .setMaxPoolSize(json.getInteger("maxPoolSize")).setConnectTimeout(json.getInteger("timeout")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != pushIn)
                try {
                    pushIn.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }


    /**
     * @Description 配置IOS push服务
     * @author zhang bo
     * @date 17-11-23
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void loadIOSConf(Vertx vertx) {
        InputStream pushIn = SMSClient.class.getResourceAsStream("/push.json");
        String pushConf = "";//jdbc连接配置
        Buffer buffer;
        try {
            InputStream jksIn;
            String host = "";
            if (System.getProperty("pushDev").equalsIgnoreCase("true")) {//推送生產
                jksIn = PushClient.class.getResourceAsStream("/kaadas_push.p12");
                host = ConstantConf.PUSH_API;
            } else {
                jksIn = PushClient.class.getResourceAsStream("/kaadas_dev.p12");
                host = ConstantConf.DEVELOP_API;
            }
            byte[] jksByte = IOUtils.toByteArray(jksIn);
            buffer = Buffer.buffer().appendBytes(jksByte);
            pushConf = IOUtils.toString(pushIn, "UTF-8");//获取配置
            if (!pushConf.equals("")) {
                JsonObject json = new JsonObject(pushConf);
                iosClient = io.vertx.ext.web.client.WebClient.create(vertx, new WebClientOptions()
                        .setDefaultPort(json.getInteger("port")).setDefaultHost(host)
                        .setVerifyHost(json.getBoolean("verifyHost")).setSsl(json.getBoolean("is_ssl"))
                        .setTrustAll(json.getBoolean("trustAll"))
                        .setProtocolVersion(HttpVersion.HTTP_2)
                        .setUseAlpn(json.getBoolean("alpn"))
                        .setMaxPoolSize(json.getInteger("maxPoolSize")).setConnectTimeout(json.getInteger("timeout"))
                        .setPfxKeyCertOptions(new PfxOptions().setValue(buffer).setPassword(json.getString("cert_password")))
                        .setIdleTimeout(json.getInteger("IdleTimeout")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != pushIn)
                try {
                    pushIn.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }
}
