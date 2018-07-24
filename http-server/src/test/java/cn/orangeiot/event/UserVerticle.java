package cn.orangeiot.event;

import cn.orangeiot.event.service.ModelTest;
import io.vertx.core.*;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.ResponseContentTypeHandler;
import io.vertx.spi.cluster.zookeeper.ZookeeperClusterManager;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.curator.framework.CuratorFramework;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.util.*;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-07-17
 */
public class UserVerticle extends AbstractVerticle {

    private Router router;

    private UserService userService;

    private Vertx vertx;

    private JsonObject json;

    public static void main(String[] args) {
        Vertx.vertx().deployVerticle(UserVerticle.class.getName(), rs -> {
            if (rs.failed()) {
                rs.cause().printStackTrace();
            } else {
                System.out.println("deploy success");
            }
        });
    }


    /**
     * @Description
     * @author zhang bo
     * @date 17-12-8
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void loadClusting() {
        json = new JsonObject()
                .put("hosts.zookeeper", "127.0.0.1:2181").put("sessionTimeout", 5000).put("connectTimeout", 10000)
                .put("rootPath", "test").put("retry", new JsonObject().put("initialSleepTime", 100)
                        .put("initialSleepTime", 10000).put("maxTimes", 5));

        System.setProperty("vertx.zookeeper.hosts", json.getString("hosts.zookeeper"));
        ClusterManager mgr = new ZookeeperClusterManager(json);
        VertxOptions options = new VertxOptions().setClusterManager(mgr);
        if (Objects.nonNull(json.getValue("node.host")))
            options.setClusterHost(json.getString("node.host"));

        //集群
        Vertx.clusteredVertx(options, res -> {
            if (res.succeeded()) {
                vertx = res.result();
                createHttpServer();
            } else {
                System.exit(1);
                System.err.println("clusting fail");
            }
        });
    }


    @Override
    public void start(Future<Void> startFuture) throws Exception {
        loadClusting();
        startFuture.complete();
    }


    @SuppressWarnings("Duplicates")
    public void createHttpServer() {
        router = Router.router(vertx);
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

        vertx.createHttpServer(
                new HttpServerOptions().setCompressionSupported(true))
                .requestHandler(router::accept).listen(11124, "0.0.0.0");


        createRouting();
        scanAnnaction(UserService.class);


        vertx.setPeriodic(1000, id -> {
            System.out.println("And every second this is printed");
        });
    }


    /**
     * @Description 創建路由
     * @author zhang bo
     * @date 18-7-17
     * @version 1.0
     */
    public void createRouting() {
        userService = new UserServiceImpl();
        router.get("/test").blockingHandler(userService::test);
        router.get("/hello").blockingHandler(userService::hello);
    }


    /**
     * @Description 扫描注解
     * @author zhang bo
     * @date 18-7-17
     * @version 1.0
     */
    public void scanAnnaction(Class<?> cl) {
        ModelTest modelTest = new ModelTest();
        byte[] dataList = new byte[1024];
        for (Method method : cl.getDeclaredMethods()) {
            userInterFace record = method.getAnnotation(userInterFace.class);
            if (record != null) {
                modelTest.setMethod(record.requsetMethod()).setUrl(record.url());
            }
        }
        modelTest.addHandler(userService::test).addHandler(userService::hello);

        try {
            dataList = serialize(modelTest);
        } catch (Exception e) {
            e.printStackTrace();
        }

        vertx.eventBus().send("cn.orange.registerHandler", dataList, new DeliveryOptions()
                .setSendTimeout(3000), (AsyncResult<Message<JsonObject>> rs) -> {
            if (rs.failed()) {
                rs.cause().printStackTrace();
            } else {
                if (Objects.nonNull(rs.result().body().getValue("code"))
                        && rs.result().body().getInteger("code") == 200) {
                    System.out.println("publish api success");
                } else {
                    System.out.println("publish api fail");
                }
            }
        });
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
