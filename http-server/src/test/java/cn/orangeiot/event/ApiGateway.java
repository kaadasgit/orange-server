package cn.orangeiot.event;

import cn.orangeiot.common.constant.HttpAttrType;
import cn.orangeiot.common.genera.ErrorType;
import cn.orangeiot.common.genera.Result;
import cn.orangeiot.event.service.ModelTest;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.ResponseContentTypeHandler;
import io.vertx.ext.web.handler.TimeoutHandler;
import io.vertx.spi.cluster.zookeeper.ZookeeperClusterManager;
import org.apache.commons.io.output.ByteArrayOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-07-17
 */
public class ApiGateway extends AbstractVerticle {

    private Router router;

    private Vertx vertx;

    public static void main(String[] args) {
        Vertx.vertx().deployVerticle(ApiGateway.class.getName(), rs -> {
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
        JsonObject json = new JsonObject()
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

    /**
     * @Description 创建服务器
     * @author zhang bo
     * @date 18-7-16
     * @version 1.0
     */
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

        ExceptionAndTimeout(router);
        router.route().handler(BodyHandler.create());
        router.route("/*").handler(ResponseContentTypeHandler.create());

        vertx.createHttpServer(
                new HttpServerOptions().setCompressionSupported(true))
                .requestHandler(router::accept).listen(11123, "0.0.0.0");

        registerHandler();
    }


    /**
     * @Description API 事件注册
     * @author zhang bo
     * @date 18-7-17
     * @version 1.0
     */
    public void registerHandler() {
        vertx.eventBus().consumer("cn.orange.registerHandler", (Message<byte[]> msg) -> {
            if (Objects.nonNull(msg.body())) {//數據校驗
                try {
                    ModelTest modelTest = (ModelTest) deserialize(msg.body());

                    int index = 0;
                    for (String url : modelTest.getUrl()) {
                        router.get(url).blockingHandler(modelTest.getListsHandler().get(index));
                        index++;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    msg.reply(new JsonObject().put("code", 500).put("msg", "service fail"));
                }
                msg.reply(new JsonObject().put("code", 200).put("msg", "ok"));
            } else {
                msg.reply(new JsonObject().put("code", 407).put("msg", "don't data fail"));
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


    /**
     * 全局异常和超时处理
     */
    @SuppressWarnings("Duplicates")
    public void ExceptionAndTimeout(Router router) {
        /** 全局异常处理*/
        router.route("/*").failureHandler(failureRoutingContext -> {
            int statusCode = failureRoutingContext.statusCode();//获取错误状态码

            HttpServerRequest request = failureRoutingContext.request();
            HttpServerResponse response = failureRoutingContext.response();
            Result<Object> result = new Result<>();
            response.putHeader(HttpAttrType.CONTENT_TYPE_JSON.getKey(), HttpAttrType.CONTENT_TYPE_JSON.getValue());
            if (statusCode >= 0) {//判断是否出现异常
                switch (statusCode) {//全局异常处理
                    case 404:
                        result.setErrorMessage(ErrorType.RESULT_RESOURCES_NOT_FIND.getKey(), ErrorType.RESULT_RESOURCES_NOT_FIND.getValue());
                        response.end(JsonObject.mapFrom(result).toString());
                        break;
                    case 500:
                        result.setErrorMessage(ErrorType.RESULT_LOGIN_EVIL.getKey(), ErrorType.RESULT_LOGIN_EVIL.getValue());
                        result.setErrorMessage(ErrorType.RESULT_SERVER_FIAL.getKey(), ErrorType.RESULT_SERVER_FIAL.getValue());
                        response.end(JsonObject.mapFrom(result).toString());
                        break;
                    case 801://恶意登录，不是正确的token
                        result.setErrorMessage(ErrorType.RESULT_LOGIN_EVIL.getKey(), ErrorType.RESULT_LOGIN_EVIL.getValue());
                        response.end(JsonObject.mapFrom(result).toString());
                        break;
                    case 806://参数格式不正确
                        result.setErrorMessage(ErrorType.RESULT_LOGIN_ILLEGAL.getKey(), ErrorType.RESULT_LOGIN_ILLEGAL.getValue());
                        response.end(JsonObject.mapFrom(result).toString());
                        break;
                    case 509://處理超時
                        result.setErrorMessage(ErrorType.RESULT_SERVER_TIME_OUT.getKey(), ErrorType.RESULT_SERVER_TIME_OUT.getValue());
                        response.end(JsonObject.mapFrom(result).toString());
                        break;
                    case 501://业务处理失败
                        result.setErrorMessage(ErrorType.REQUIRED_PRECESSS_FAIL.getKey(), ErrorType.REQUIRED_PRECESSS_FAIL.getValue());
                        response.end(JsonObject.mapFrom(result).toString());
                        break;
                    case 401://必需的参数没有传或参数类型不对null值
                        result.setErrorMessage(ErrorType.RESULT_PARAMS_FAIL.getKey(), ErrorType.RESULT_PARAMS_FAIL.getValue());
                        response.end(JsonObject.mapFrom(result).toString());
                        break;
                    case 413://body体积太大
                        result.setErrorMessage(ErrorType.BODY_SIZE_FAIL.getKey(), ErrorType.BODY_SIZE_FAIL.getValue());
                        response.end(JsonObject.mapFrom(result).toString());
                        break;
                    default:
                        result.setErrorMessage(ErrorType.RESULT_UNKONWN.getKey(), ErrorType.RESULT_UNKONWN.getValue());
                        response.end(JsonObject.mapFrom(result).toString());
                        break;
                }
                //写入错误日志
                if (Objects.nonNull(failureRoutingContext.failure()))
                    System.err.println("uri -> " + request.uri() + "  Exception ->" + failureRoutingContext.failure());//打印异常消息
                else
                    System.err.println("uri -> " + request.uri() + "  Exception ->" + JsonObject.mapFrom(result).toString());//打印异常消息
            } else {//打印异常日志
                result.setErrorMessage(ErrorType.RESULT_SERVER_FIAL.getKey(), ErrorType.RESULT_SERVER_FIAL.getValue());
                response.end(JsonObject.mapFrom(result).toString());
                System.err.println("uri -> " + request.uri() + "  Exception ->" + failureRoutingContext.failure());//打印异常消息
            }
        }).handler(TimeoutHandler.create(2000, 509));//超时处理,返回错误码;

    }

}
