package cn.orangeiot;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.*;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-12-20
 */
public class Test extends AbstractVerticle {


    @SuppressWarnings("Duplicates")
    public static void main(String[] args) {

//        System.out.println(LocalDate.now().get(ChronoField.ALIGNED_WEEK_OF_YEAR));//获取当前年的第几周
//        Buffer buffer = Test.loadConf();
//
//
//        WebClient webClient = WebClient.create(Vertx.vertx(), new WebClientOptions().setConnectTimeout(2000)
//                .setMaxPoolSize(100)
//                .setSsl(true).setVerifyHost(false).setTrustAll(true));
//
//        String random = KdsCreateRandom.createRandom(10);
//        String code = KdsCreateRandom.createRandom(6);
//        Long time = System.currentTimeMillis() / 1000;
//        String mobile = "13510513746";
//        String appid = "1400034036";
//        String appkey = "3baf2aa7c9cdede6f90545995d68b959";
//        String str = getSHA256Str("appkey=APPKEY&random=RANDOM&time=TIME&mobile=MOBILE"
//                .replace("APPKEY", appkey).replace("RANDOM"
//                        , random).replace("TIME", time.toString()).replace("MOBILE", mobile));
//
//
//        SHA256.getSHA256Str("appkey=APPKEY&random=RANDOM&time=TIME&mobile=MOBILE"
//                .replace("APPKEY", appkey).replace("RANDOM"
//                        , random).replace("TIME", time.toString()).replace("MOBILE", mobile), as -> {
//            if (as.failed()) {
//                as.cause().printStackTrace();
//            } else {
//                JsonObject jsonObject = new JsonObject();
//                jsonObject.put("tel", new JsonObject().put("nationcode", "86").put("mobile", mobile))
//                        .put("tpl_id", 25814).put("params", new JsonArray().add(code))
//                        .put("sig", as.result()).put("time", time).put("extend", "").put("ext", "");
//
//                webClient.post(443, "183.61.56.115", "/v5/tlssmssvr/sendsms")
//                        .addQueryParam("sdkappid", appid)
//                        .addQueryParam("random", random)
//                        .sendJsonObject(jsonObject, rs -> {
//                            if (rs.failed()) {
//                                rs.cause().printStackTrace();
//                            } else {
//                                System.out.println(rs.result().body());
//                            }
//                        });
//            }
//        });

        Vertx vertx = Vertx.vertx();
        Buffer buffer = Test.loadConf();
//        WebClient webClient = WebClient.create(vertx, new WebClientOptions()
//                .setMaxPoolSize(5000).setKeepAlive(true).setDefaultHost("47.106.87.6").setDefaultPort(8090));

        for (int i = 0; i <= 10000; i++) {
            WebClient webClient = WebClient.create(vertx, new WebClientOptions()
                    .setMaxPoolSize(1).setKeepAlive(true));
//            webClient.post(8090, "47.106.87.6", "/user/login/getuserbytel")
//                    .putHeader("Content-Type", "application/json")
//                    .sendJsonObject(new JsonObject().put("tel", "13510513746").put("password", "zb123456"), rs -> {
//                        if (rs.failed()) {
//                            rs.cause().printStackTrace();
//                        } else {
//                            System.out.println(rs.result().body().toString());
//                        }
//                    });
            webClient.get(8090, "47.106.87.6","/ceshi").send(rs -> {
                if (Objects.nonNull(rs.result()))
                    System.out.println(rs.result().body());
            });
        }

        vertx.setPeriodic(100000,id->{
            for (int i = 0; i <= 10000; i++) {
                WebClient webClient = WebClient.create(vertx, new WebClientOptions()
                        .setMaxPoolSize(1).setKeepAlive(true));
                webClient.get(8090, "47.106.87.6","/ceshi").send(rs -> {
                    if (Objects.nonNull(rs.result()))
                        System.out.println(rs.result().body());
                });
            }
        });

//        WebClient webClient = WebClient.create(Vertx.vertx(), new WebClientOptions().setConnectTimeout(2000)
//                .setMaxPoolSize(100).setDefaultPort(443)
//                .setSsl(true).setVerifyHost(false).setTrustAll(true));
//
//        webClient.post("api.jpush.cn", "/v3/push")
//                .putHeader("Content-Type", "application/json")
//                .putHeader("Authorization", "Basic YWEzYTAyNDk1N2E5MWNkZGU0OGM3ZDk1Ojk3OGVkMDdkNjk3ZWM5OTJkNmRkM2MyMw==")
//                .sendJsonObject(new JsonObject().put("platform", "all")
//                        .put("audience", new JsonObject().put("registration_id"
//                                , new JsonArray().add("121c83f760020ee299d")))
//                        .put("notification", new JsonObject().put("alert", "Hello, JPush!")), rs -> {
//                    if (rs.failed()) {
//                        rs.cause().printStackTrace();
//                    } else {
//                        System.out.println(rs.result().body().toString());
//                    }
//                });


//        /**APNS推送需要的证书、密码、和设备的Token**/
//        String  p12Path = "/home/linuxzhangbo/project/orangeiot/http-server/src/main/resources/kaadas_push.p12";
//        String  password = "123456";
//        String  pushToken = "25dedf1b f61c686d 93c8b7fc 64768903 45751e5b fff8e69d 57f85efd e64f9f87";
//
//        try {
//            /**设置参数，发送数据**/
//            ApnsService service = APNS.newService().withCert(p12Path,password).withSandboxDestination().build();
//            String payload = APNS.newPayload().alertBody("hello,www.mbaike.net").badge(1).sound("default").build();
//            service.push(pushToken, payload);
//            System.out.println("推送信息已发送！");
//        } catch (Exception e) {
//            System.out.println("出错了："+e.getMessage());
//        }


//        String deviceToken = "ca0bca19fc39064eb615ef0f0aef7261027f9e13f68192b9267beaf5c229dbb8";
//
//        InputStream jksIn = Test.class.getResourceAsStream("/kaadas_dev.p12");
//        Buffer buffer = null;
//        try {
//            byte[] jksByte = IOUtils.toByteArray(jksIn);
//            buffer = Buffer.buffer().appendBytes(jksByte);
//            WebClient webClient = WebClient.create(Vertx.vertx(), new WebClientOptions().setConnectTimeout(2000)
//                    .setMaxPoolSize(100).setDefaultPort(443).setUseAlpn(true).setTrustAll(true)
//                    .setSsl(true).setVerifyHost(false).setProtocolVersion(HttpVersion.HTTP_2)
//                    .setPfxKeyCertOptions(new PfxOptions().setValue(buffer).setPassword("123456")));
//
//            JsonObject params = new JsonObject().put("aps", new JsonObject().put("alert", "Hello world"));
//
//            webClient.post("api.development.push.apple.com"
//                    , "/3/device/" + deviceToken)
//                    .putHeader("Content-Type", "application/json")
//                    .putHeader("apns-topic", "com.kaidishi.lock")
//                    .putHeader("apns-id", UUID.randomUUID().toString())
//                    .putHeader("Content-length", String.valueOf(params.toString().getBytes().length))
//                    .sendJsonObject(params, rs -> {
//                        if (rs.failed()) {
//                            rs.cause().printStackTrace();
//                        } else {
//                            System.out.println("status ->" + rs.result().statusCode());
//                            if (Objects.nonNull(rs.result().body()))
//                                System.out.println(rs.result().body().toString());
//                        }
//                    });
//        } catch (Exception e) {
//            logger.error(e.getMessage(), e)();
//        }

//        final Base64.Decoder decoder = Base64.getDecoder();
//        final Base64.Encoder encoder = Base64.getEncoder();
//        final String text = "339089998f183b226b657010:65eaac4d56d769a506a1cda8";
//        try {
//            final byte[] textByte = text.getBytes("UTF-8");
//            //编码
//            final String encodedText = encoder.encodeToString(textByte);
//            System.out.println(encodedText);
//
//            //解码
//            System.out.println(new String(decoder.decode(encodedText), "UTF-8"));
//
//        } catch (UnsupportedEncodingException e) {
//            logger.error(e.getMessage(), e)();
//        }
    }


    //    @SuppressWarnings("Duplicates")
//    public static String SN(int count, int batch, int cishu) {
//        String sn = "";
//        int final_num = 9999;
//        boolean flag = (count + cishu) > final_num ? true : false;//余下条数
//        int yushu = (count + cishu) > final_num ? (final_num - cishu) : cishu;//余下条数
//        int ontshu = (count + cishu) > final_num ? count - (final_num - cishu) : cishu;//余下第一次条数
//
//        List<String> snList = new ArrayList<>();
//        List<String> strList = new ArrayList<>();
//        if (flag) {//第一情情況超过最大值
//            int dayWeek = LocalDate.now().get(ChronoField.ALIGNED_WEEK_OF_YEAR);//获取周代码
//            String week = String.valueOf(dayWeek).length() == 1 ? "0" + String.valueOf(dayWeek) : String.valueOf(dayWeek);
//            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yy"));//获取年代码
//            //第一次区间生成
//            for (int i = cishu + 1; i <= final_num; i++) {
//                snList.add("ogyi" + time + week + val(i) + batch);
//                strList.add(String.valueOf(i));
//            }
//            int pici;
//            int startNum;
//            if (ontshu > final_num) {
//                pici = ontshu % final_num == 0 ? ontshu / final_num : ontshu / final_num + 1;//一共多少批次
//                startNum = ontshu - (final_num * (ontshu / final_num));//最后次数的开始阶段
//            } else {
//                pici = 1;//一共多少批次
//                startNum = ontshu;
//            }
//            //余下区间生成
//            for (int i = batch + 1; i <= (pici + batch); i++) {
//                if (i == (pici + batch)) {
//                    for (int j = 1; j <= startNum; j++) {
//                        snList.add("ogyi" + time + week + val(j) + i);
//                    }
//                } else {
//                    for (int j = 1; j <= final_num; j++) {
//                        snList.add("ogyi" + time + week + val(j) + i);
//                    }
//                }
//            }
//        } else {
//            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yy"));//获取年代码
//            int dayWeek = LocalDate.now().get(ChronoField.ALIGNED_WEEK_OF_YEAR);//获取周代码
//            String week = String.valueOf(dayWeek).length() == 1 ? "0" + String.valueOf(dayWeek) : String.valueOf(dayWeek);
//            for (int i = cishu + 1; i <= (cishu + count); i++) {
//                snList.add("ogyi" + time + week + val(i) + batch);
//            }
//        }
//        return sn;
//    }
//
//
//    @SuppressWarnings("Duplicates")
//    public static String val(int count) {
//        String str = "";
//        switch (String.valueOf(count).length()) {
//            case 1:
//                str = "000" + String.valueOf(count);
//                break;
//            case 2:
//                str = "00" + String.valueOf(count);
//                break;
//            case 3:
//                str = "0" + String.valueOf(count);
//                break;
//            default:
//                str = String.valueOf(count);
//                break;
//        }
//        return str;
//    }
//
    public static Buffer loadConf() {
        InputStream jksIn = Test.class.getResourceAsStream("/server.cer");
        Buffer buffer = null;
        try {
            byte[] jksByte = IOUtils.toByteArray(jksIn);
            buffer = Buffer.buffer().appendBytes(jksByte);
        } catch (IOException e) {
//            logger.error(e.getMessage(), e);
        }
        return buffer;
    }
}
