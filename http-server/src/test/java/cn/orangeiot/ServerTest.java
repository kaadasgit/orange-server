package cn.orangeiot;

import cn.orangeiot.http.spi.SpiConf;
import cn.orangeiot.http.verticle.HttpServerVerticle;
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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-12-20
 */
public class ServerTest extends AbstractVerticle{


    @Override
    public void start(Future<Void> startFuture) throws Exception {
        Router router = Router.router(vertx);
        InputStream jksIn = SpiConf.class.getResourceAsStream("/ser.jks");
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

            router.get("/ceshi").handler(routingContext -> {
                System.out.println("=======ceshi");
                routingContext.response().putHeader("content-type","application/json")
                        .end(new JsonObject().put("code",200).toString());
            });

            router.post("/hello").produces("application/json").handler(routingContext -> {
                System.out.println("=======test");
                routingContext.response().end(new JsonObject().put("code",200).toString());
            });

            byte[] jksByte = IOUtils.toByteArray(jksIn);
            buffer = Buffer.buffer().appendBytes(jksByte);
            vertx.createHttpServer(
                    new HttpServerOptions().setCompressionSupported(true).setSsl(true)
                            .setKeyStoreOptions(new JksOptions().setValue(buffer)
                                    .setPassword("123456")))
                    .requestHandler(router::accept).listen(11123,"0.0.0.0");
        } catch (IOException e) {
            e.printStackTrace();
        }
        startFuture.complete();
    }


    public static void main(String[] args){
        Vertx.vertx().deployVerticle(ServerTest.class.getName(), rs->{
            if(rs.failed()){
                rs.cause().printStackTrace();
            }else{
            }
        });
    }
}
