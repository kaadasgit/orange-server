package cn.orangeiot;

import cn.orangeiot.common.utils.KdsCreateRandom;
import cn.orangeiot.common.utils.SHA256;
import cn.orangeiot.http.spi.SpiConf;
import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.PfxOptions;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Array;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalField;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-12-20
 */
public class Test extends AbstractVerticle {


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

        Buffer buffer = Test.loadConf();
        WebClient webClient = WebClient.create(Vertx.vertx(), new WebClientOptions().setConnectTimeout(2000)
                .setMaxPoolSize(100)
                .setSsl(true)
                .setPfxTrustOptions(new PfxOptions().setValue(buffer).setPassword("123456"))
                .setVerifyHost(false));

        webClient.post(8090, "114.67.58.242", "/user/login/getuserbytel")
                .putHeader("Content-Type", "application/json")
                .sendJsonObject(new JsonObject().put("tel", "13510513746").put("password", "zb123456"), rs -> {
                    if (rs.failed()) {
                        rs.cause().printStackTrace();
                    } else {
                        System.out.println(rs.result().body().toString());
                    }
                });


    }


    @SuppressWarnings("Duplicates")
    public static String SN(int count, int batch, int cishu) {
        String sn = "";
        int final_num = 9999;
        boolean flag = (count + cishu) > final_num ? true : false;//余下条数
        int yushu = (count + cishu) > final_num ? (final_num - cishu) : cishu;//余下条数
        int ontshu = (count + cishu) > final_num ? count - (final_num - cishu) : cishu;//余下第一次条数

        List<String> snList = new ArrayList<>();
        List<String> strList = new ArrayList<>();
        if (flag) {//第一情情況超过最大值
            int dayWeek = LocalDate.now().get(ChronoField.ALIGNED_WEEK_OF_YEAR);//获取周代码
            String week = String.valueOf(dayWeek).length() == 1 ? "0" + String.valueOf(dayWeek) : String.valueOf(dayWeek);
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yy"));//获取年代码
            //第一次区间生成
            for (int i = cishu + 1; i <= final_num; i++) {
                snList.add("ogyi" + time + week + val(i) + batch);
                strList.add(String.valueOf(i));
            }
            int pici;
            int startNum;
            if (ontshu > final_num) {
                pici = ontshu % final_num == 0 ? ontshu / final_num : ontshu / final_num + 1;//一共多少批次
                startNum = ontshu - (final_num * (ontshu / final_num));//最后次数的开始阶段
            } else {
                pici = 1;//一共多少批次
                startNum = ontshu;
            }
            //余下区间生成
            for (int i = batch + 1; i <= (pici + batch); i++) {
                if (i == (pici + batch)) {
                    for (int j = 1; j <= startNum; j++) {
                        snList.add("ogyi" + time + week + val(j) + i);
                    }
                } else {
                    for (int j = 1; j <= final_num; j++) {
                        snList.add("ogyi" + time + week + val(j) + i);
                    }
                }
            }
        } else {
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yy"));//获取年代码
            int dayWeek = LocalDate.now().get(ChronoField.ALIGNED_WEEK_OF_YEAR);//获取周代码
            String week = String.valueOf(dayWeek).length() == 1 ? "0" + String.valueOf(dayWeek) : String.valueOf(dayWeek);
            for (int i = cishu + 1; i <= (cishu + count); i++) {
                snList.add("ogyi" + time + week + val(i) + batch);
            }
        }
        return sn;
    }


    @SuppressWarnings("Duplicates")
    public static String val(int count) {
        String str = "";
        switch (String.valueOf(count).length()) {
            case 1:
                str = "000" + String.valueOf(count);
                break;
            case 2:
                str = "00" + String.valueOf(count);
                break;
            case 3:
                str = "0" + String.valueOf(count);
                break;
            default:
                str = String.valueOf(count);
                break;
        }
        return str;
    }

    public static Buffer loadConf() {
        InputStream jksIn = Test.class.getResourceAsStream("/server.p12");
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
