package cn.orangeiot;

import cn.orangeiot.http.spi.SpiConf;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.PfxOptions;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-12-20
 */
public class Test extends AbstractVerticle {


    public static void main(String[] args) {
        Buffer buffer = Test.loadConf();

        WebClient webClient = WebClient.create(Vertx.vertx(), new WebClientOptions().setConnectTimeout(2000)
                .setMaxPoolSize(100)
                .setSsl(true).setVerifyHost(false)
                .setPfxTrustOptions(new PfxOptions().setValue(buffer).setPassword("123456")));

        webClient.post(8090, "192.168.6.87", "/mail/sendemailtoken").putHeader("contentType", "application/json")
                .sendJsonObject(new JsonObject().put("mail", "564739784@qq.com"), rs -> {
                    if (rs.failed()) {
                        rs.cause().printStackTrace();
                    } else {
                        System.out.println(rs.result().body());
                    }
                });


//        WebClient webClient = WebClient.create(Vertx.vertx(), new WebClientOptions().setConnectTimeout(2000)
//                .setMaxPoolSize(100)
//                .setSsl(true)
//                .setPfxTrustOptions(new PfxOptions().setValue(buffer).setPassword("123456")));
//
//        webClient.post(11123, "192.168.6.87", "/hello")
//                .send(rs -> {
//                    if (rs.failed()) {
//                        rs.cause().printStackTrace();
//                    } else {
//                        System.out.println(rs.result().body());
//                    }
//                });

    }

    public static Buffer loadConf() {
        InputStream jksIn = SpiConf.class.getResourceAsStream("/server.p12");
        Buffer buffer = null;
        try {
            byte[] jksByte = IOUtils.toByteArray(jksIn);
            buffer = Buffer.buffer().appendBytes(jksByte);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return buffer;
    }
}
