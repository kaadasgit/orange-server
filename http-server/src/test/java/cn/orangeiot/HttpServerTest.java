package cn.orangeiot;

import cn.orangeiot.http.spi.SpiConf;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.ResponseContentTypeHandler;
import org.apache.commons.io.output.ByteArrayOutputStream;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-07-16
 */
public class HttpServerTest extends AbstractVerticle {

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        Vertx.vertx().deployVerticle(HttpServerTest.class.getName(), rs -> {
            if (rs.failed()) {
                rs.cause().printStackTrace();
            } else {
                System.out.println("deploy success");
                trackLogRecord(HttpServerTest.class, vertx);
            }
        });
    }


    @Override
    public void start(Future<Void> startFuture) throws Exception {
        createHttpServer();
        startFuture.complete();
    }

    /**
     * @Description 创建服务器
     * @author zhang bo
     * @date 18-7-16
     * @version 1.0
     */
    public void createHttpServer() {
        Router router = Router.router(vertx);
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
            routingContext.response().putHeader("content-type", "application/json")
                    .end(new JsonObject().put("code", 200).toString());
        });


        router.post("/hello").produces("application/json").handler(this::test);

        vertx.createHttpServer(
                new HttpServerOptions().setCompressionSupported(true))
                .requestHandler(router::accept).listen(11123, "0.0.0.0");
    }


    @userInterFace(url = "/hello", requsetMethod = "post")
    public void test(RoutingContext routingContext) {
        System.out.println("=======hello");
        routingContext.response().putHeader("content-type", "application/json")
                .end(new JsonObject().put("code", 200).toString());
    }


    public static void trackLogRecord(Class<?> cl, Vertx vertx) {
        for (Method method : cl.getDeclaredMethods()) {
            userInterFace logRecord = method.getAnnotation(userInterFace.class);
            if (logRecord != null) {
                System.out.println("log recode -> " + logRecord.url() + "  ,  " + logRecord.requsetMethod());
                try {
                    method.invoke(cl.newInstance(),null);

                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                }
            }
        }

    }


    /**
     * java序列化
     *
     * @param obj
     * @return
     * @throws Exception
     */
    public static byte[] serialize(Object obj) throws Exception {
        if (obj == null)
            throw new NullPointerException();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(os);
        out.writeObject(obj);
        return os.toByteArray();
    }

    /**
     * java反序列化
     *
     * @param by
     * @return
     * @throws Exception
     */
    public static Object deserialize(byte[] by) throws Exception {
        if (by == null)
            throw new NullPointerException();
        ByteArrayInputStream is = new ByteArrayInputStream(by);
        ObjectInputStream in = new ObjectInputStream(is);
        return in.readObject();

    }
}
