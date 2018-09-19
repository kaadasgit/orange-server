package cn.servertest;

import cn.orangeiot.ServerTest;
import cn.orangeiot.common.constant.HttpAttrType;
import cn.orangeiot.common.genera.Result;
import cn.orangeiot.http.spi.SpiConf;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.ResponseContentTypeHandler;
import org.apache.commons.io.IOUtils;
import scala.util.parsing.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-09-19
 */
@SuppressWarnings("Duplicates")
public class ManagentServerTest extends AbstractVerticle {

    final String token = "aswqewqd2131";//appscript
    final String host = "0.0.0.0";
    final int port = 8070;//端口

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        Router router = Router.router(vertx);
        InputStream jksIn = SpiConf.class.getResourceAsStream("/server.jks");
        Buffer buffer = null;
        try {
            Set<String> allowHeaders = new HashSet<>();//cors跨域header

            Set<HttpMethod> allowMethods = new HashSet<>();//请求方法

            allowMethods.add(HttpMethod.GET);
            allowMethods.add(HttpMethod.POST);
            allowMethods.add(HttpMethod.DELETE);
            allowMethods.add(HttpMethod.PUT);

            allowHeaders.add("Content-Type");
            router.route("/*").handler(CorsHandler.create("*")//设置请求域Origin的范围*(reqeust不能带上cookies)代表所有
                    .allowedHeaders(allowHeaders)
                    .allowedMethods(allowMethods));

            router.route().handler(BodyHandler.create());
            router.route("/*").handler(ResponseContentTypeHandler.create());

            //获取用户rest服务在线人数
            router.post("/user/state/online/count").produces("application/json").handler(routingContext -> {
                if (Objects.nonNull(routingContext.request().getHeader("auth")) && routingContext.request().getHeader("auth").equals(token)) {
                    routingContext.response().putHeader("content-type", "application/json")
                            .end(JsonObject.mapFrom(new Result<JsonObject>().setData(new JsonObject().put("count", 5).put("totalcount", 100))).toString());
                } else {
                    routingContext.response().putHeader("content-type", "application/json").setStatusCode(401)
                            .end(JsonObject.mapFrom(new Result<JsonObject>().setErrorMessage(401, "auth fail")).toString());
                }

            });

            //mqttbroker 在线数量
            router.post("/mqttbroker/online/count").produces("application/json").handler(routingContext -> {
                if (Objects.nonNull(routingContext.request().getHeader("auth")) && routingContext.request().getHeader("auth").equals(token)) {
                    routingContext.response().putHeader("content-type", "application/json")
                            .end(JsonObject.mapFrom(new Result<JsonObject>().setData(new JsonObject().put("count", 132).put("totalcount", 150))).toString());
                } else {
                    routingContext.response().putHeader("content-type", "application/json").setStatusCode(401)
                            .end(JsonObject.mapFrom(new Result<JsonObject>().setErrorMessage(401, "auth fail")).toString());
                }
            });


            //根据时间区间获取注册人数
            router.post("/user/register/interTime/count").produces("application/json").handler(routingContext -> {
                if (Objects.nonNull(routingContext.request().getHeader("auth")) && routingContext.request().getHeader("auth").equals(token)) {
                    routingContext.response().putHeader("content-type", "application/json")
                            .end(JsonObject.mapFrom(new Result<JsonObject>().setData(new JsonObject().put("count", 700))).toString());
                } else {
                    routingContext.response().putHeader("content-type", "application/json").setStatusCode(401)
                            .end(JsonObject.mapFrom(new Result<JsonObject>().setErrorMessage(401, "auth fail")).toString());
                }
            });

            byte[] jksByte = IOUtils.toByteArray(jksIn);
            buffer = Buffer.buffer().appendBytes(jksByte);
            vertx.createHttpServer(
                    new HttpServerOptions().setCompressionSupported(true).setSsl(true)
                            .setKeyStoreOptions(new JksOptions().setValue(buffer)
                                    .setPassword("123456")))
                    .requestHandler(router::accept).listen(port, host);
        } catch (IOException e) {
            e.printStackTrace();
        }
        startFuture.complete();
    }


    public static void main(String[] args) {
        Vertx.vertx().deployVerticle(ManagentServerTest.class.getName(), rs -> {
            if (rs.failed()) {
                rs.cause().printStackTrace();
            } else {
                System.out.println("deploy success");
            }
        });
    }
}
