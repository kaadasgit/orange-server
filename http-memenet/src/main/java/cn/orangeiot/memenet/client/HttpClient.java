package cn.orangeiot.memenet.client;

import cn.orangeiot.common.utils.KdsCreateRandom;
import cn.orangeiot.common.utils.SHA256;
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
 * @date 2017-12-28
 */
public class HttpClient {

    public static io.vertx.ext.web.client.WebClient client;

    private static Logger logger = LogManager.getLogger(HttpClient.class);

    /**
     * @Description redisClient配置
     * @author zhang bo
     * @date 17-11-23
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void webClientConf(Vertx vertx) {
        InputStream mongoIn = HttpClient.class.getResourceAsStream("/httpClient-conf.json");
        String httpConf = "";//jdbc连接配置
        try {
            httpConf = IOUtils.toString(mongoIn, "UTF-8");//获取配置

            if (!httpConf.equals("")) {
                JsonObject json = new JsonObject(httpConf);
                client = io.vertx.ext.web.client.WebClient.create(vertx, new WebClientOptions()
                        .setDefaultHost(json.getString("host")).setDefaultPort(json.getInteger("port"))
                        .setVerifyHost(json.getBoolean("verifyHost")).setSsl(json.getBoolean("is_ssl"))
                        .setTrustAll(json.getBoolean("trustAll"))
                        .setMaxPoolSize(json.getInteger("maxPoolSize")
                        ).setConnectTimeout(json.getInteger("timeout")));
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

    public static void main(String[] args) {
        String random = KdsCreateRandom.createRandom(10);//获取随机数
        SHA256.getSHA256Str("appkey=MDXD51LH6NG5M7FP2AGN&random=RANDOM_VALUE".replace("RANDOM_VALUE", random), as -> {
            if (as.failed()) {
                as.cause().printStackTrace();
            } else {
                HttpClient webClient = new HttpClient();
                webClient.webClientConf(Vertx.vertx());
                client.post("/v1/accsvr/accregister")
                        .addQueryParam("partid", "HQQ8H3HJGJ2KPQJ7NXZY")
                        .addQueryParam("appid", "AIB1EITFX0DB75MCUIZR")
                        .addQueryParam("random", random)
                        .sendJsonObject(new JsonObject().put("username", "123456")
                                .put("password", "123456").put("sig", as.result()), rs -> {
                            if (rs.failed()) {
                                logger.error(rs.cause().getMessage(), rs.cause());
                            } else {
                                System.out.println(rs.result().body());
                            }
                        });
            }
        });

    }
}
