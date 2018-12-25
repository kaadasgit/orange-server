package cn.orangeiot.verticle;

import cn.orangeiot.common.constant.HttpAttrType;
import cn.orangeiot.common.genera.ErrorType;
import cn.orangeiot.common.genera.Result;
import cn.orangeiot.mqtt.impl.SockJSSocketAndMqttSocketImpl;
import cn.orangeiot.util.DataType;
import cn.orangeiot.util.VerifyParamsUtil;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.*;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.ext.web.handler.sockjs.SockJSHandlerOptions;
import io.vertx.spi.cluster.zookeeper.ZookeeperClusterManager;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import scala.util.parsing.json.JSONArray;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-07-01
 */
public class MqttClientVerticle extends AbstractVerticle {


    private static Logger logger = LogManager.getLogger(MqttClientVerticle.class);

    private Set<String> allowHeaders = new HashSet<>();//cors跨域header

    private Set<HttpMethod> allowMethods = new HashSet<>();//请求方法

    private JsonObject config;

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        createHttpServer();
        startFuture.complete();
    }


    /**
     * @Description 创建http服务器
     * @author zhang bo
     * @date 18-7-1
     * @version 1.0
     */
    public void createHttpServer() {
        Router router = Router.router(vertx);
        enableCorsSupport(router);
        requestlog(router);
        bodyOrUpload(router);

        int port = 34113;
        String dir = "./logdir";

        if (Objects.nonNull(System.getProperty("HTTPPORT"))) {
            try {
                port = Integer.parseInt(System.getProperty("HTTPPORT"));
            } catch (Exception e) {
                System.exit(1);
                logger.error(e.getMessage(), e);
            }
        }
        if (Objects.nonNull(System.getProperty("LOGDIR")))
            dir = System.getProperty("LOGDIR");

        //文件管理
        cn.orangeiot.log.LogManager logManager = new cn.orangeiot.log.LogManager(vertx, dir);

        //配置主頁
        index(router);
        //静态资源
        staticResource(router);
        //全局配置
        globalIntercept(router);
        //全局异常处理
        ExceptionAndTimeout(router);
        //上传 api doc
        setApi(router, logManager);
        //获取所有接口
        getTitle(router, logManager);
        //获取接口详细
        getApi(router, logManager);
        //創建http服務器
        vertx.createHttpServer().requestHandler(router::accept).listen(config().getInteger("http-port", port), config().
                getString("host-name", "0.0.0.0"));

        //创建基于sockjs 的 websocket ,兼容性好
        SockJSHandlerOptions options = new SockJSHandlerOptions().setHeartbeatInterval(2000);

        SockJSHandler sockJSHandler = SockJSHandler.create(vertx, options);

        sockJSHandler.socketHandler(sockJSSocket -> new SockJSSocketAndMqttSocketImpl(sockJSSocket, vertx).start());
        router.route("/websocket/*").handler(sockJSHandler);
    }


    /**
     * @Description 定義路由api
     * @author zhang bo
     * @date 18-7-1
     * @version 1.0
     */
    public Router setApi(Router router, cn.orangeiot.log.LogManager logManager) {
        //提交api接口
        router.post("/submitApi").produces(HttpAttrType.CONTENT_TYPE_JSON.getValue())
                .consumes(HttpAttrType.CONTENT_TYPE_JSON.getValue()).blockingHandler(routingContext -> {

            VerifyParamsUtil.verifyParams(routingContext.getBodyAsJson(), new JsonObject().put("topic", DataType.STRING)
                    .put("payload", DataType.JSONOBJECT).put("title", DataType.STRING)
                    .put("qos", DataType.INTEGER).put("scheme", DataType.STRING), rs -> {
                if (rs.failed()) {
                    routingContext.fail(401);
                } else {
                    logManager.updateLog(rs.result().getString("title"), rs.result(), as -> {
                        if (as.failed()) {
                            logger.error(as.cause().getMessage(), as);
                            routingContext.fail(501);
                        } else {
                            if (as.result()) {
                                routingContext.response().end(JsonObject.mapFrom(new Result<JsonObject>()).toString());
                            } else {
                                routingContext.response().end(JsonObject.mapFrom(new Result<JsonObject>()
                                        .setErrorMessage(ErrorType.UPDATE_API_FAIL.getKey(), ErrorType.UPDATE_API_FAIL.getValue())).toString());
                            }
                        }
                    });
                }
            });
        });
        return router;
    }


    /**
     * @Description 获取接口主题
     * @author zhang bo
     * @date 18-7-3
     * @version 1.0
     */
    public void getTitle(Router router, cn.orangeiot.log.LogManager logManager) {
        //提交api接口
        router.post("/getTitle").produces(HttpAttrType.CONTENT_TYPE_JSON.getValue())
                .consumes(HttpAttrType.CONTENT_TYPE_JSON.getValue()).blockingHandler(routingContext -> {
            logManager.getDirTitle(rs -> {
                if (rs.failed()) {
                    routingContext.fail(501);
                } else {
                    routingContext.response().end(JsonObject.mapFrom(new Result<List<String>>()
                            .setData(rs.result())).toString());
                }
            });
        });
    }


    /**
     * @Description 获取接口數據
     * @author zhang bo
     * @date 18-7-3
     * @version 1.0
     */
    public void getApi(Router router, cn.orangeiot.log.LogManager logManager) {
        //提交api接口
        router.post("/getScheme").produces(HttpAttrType.CONTENT_TYPE_JSON.getValue())
                .consumes(HttpAttrType.CONTENT_TYPE_JSON.getValue()).blockingHandler(routingContext -> {

            VerifyParamsUtil.verifyParams(routingContext.getBodyAsJson(), new JsonObject().put("scheme", DataType.STRING)
                    .put("rootPath", DataType.STRING), rs -> {
                if (rs.failed()) {
                    routingContext.fail(401);
                } else {
                    logManager.getApiDoc(rs.result().getString("scheme"), rs.result().getString("rootPath"), as -> {
                        if (as.failed()) {
                            routingContext.fail(501);
                        } else {
                            routingContext.response().end(JsonObject.mapFrom(new Result<JsonArray>()
                                    .setData(as.result())).toString());
                        }
                    });
                }
            });
        });
    }

    /**
     * 全局的编码转义
     *
     * @param router
     */
    public void globalIntercept(Router router) {
        router.post("/*").handler(routingContext -> {
            logger.info("/*===globalIntercept requestUrl -> {} , params -> {} , remoteHostname -> {}"
                    , routingContext.request().uri(), routingContext.getBodyAsString(), routingContext.request().remoteAddress());
//            String params = EscapeUtils.escapeHtml(routingContext.getBodyAsString());//对特殊字符转义
            String params = routingContext.getBodyAsString().replace("\\s", "").replace("\n", "");
            if (this.paramsIsJsonObject(routingContext, params)) {
                routingContext.next();
            } else {
                routingContext.fail(806);//非法参数
            }
        }).consumes(HttpAttrType.CONTENT_TYPE_JSON.getValue());

    }


    /**
     * 参数是否是jsonObject
     */
    @SuppressWarnings("Duplicates")
    public boolean paramsIsJsonObject(RoutingContext routingContext, String params) {
        boolean flag = true;
        //验证参数
        if (Objects.nonNull(params) && params.length() > 0) {
            try {
                JsonObject jsonObject = new JsonObject(params);
            } catch (Exception e) {
                flag = false;
            }
            routingContext.put("params", params);
        }
        return flag;
    }


    /**
     * @Description 主页
     * @author zhang bo
     * @date 18-7-9
     * @version 1.0
     */
    public void index(Router router) {
        router.get("/").blockingHandler(routingContext -> {
//            routingContext.reroute("/web.html");//路由重置
            routingContext.response().setStatusCode(301).putHeader("location", "/web.html").end();//http狀態嗎301,重定向
        });
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
                    logger.error("uri -> " + request.uri() + "  Exception ->" + failureRoutingContext.failure());//打印异常消息
                else
                    logger.error("uri -> " + request.uri() + "  Exception ->" + JsonObject.mapFrom(result).toString());//打印异常消息
            } else {//打印异常日志
                result.setErrorMessage(ErrorType.RESULT_SERVER_FIAL.getKey(), ErrorType.RESULT_SERVER_FIAL.getValue());
                response.end(JsonObject.mapFrom(result).toString());
                logger.error("uri -> " + request.uri() + "  Exception ->" + failureRoutingContext.failure());//打印异常消息
            }
        });

    }


    /**
     * 跨域资源Cors配置  /[^loginCode]  可以正则匹配
     *
     * @param router
     */
    @SuppressWarnings("Duplicates")
    public void enableCorsSupport(Router router) {
        allowMethods.add(HttpMethod.GET);
        allowMethods.add(HttpMethod.POST);
        allowMethods.add(HttpMethod.DELETE);
        allowMethods.add(HttpMethod.PUT);

        allowHeaders.add("Content-Type");
        allowHeaders.add("token");
        router.route("/*").handler(CorsHandler.create("*")//设置请求域Origin的范围*(reqeust不能带上cookies)代表所有
                .allowedHeaders(allowHeaders)
                .allowedMethods(allowMethods));
    }

    /**
     * 配置Body
     */
    public void bodyOrUpload(Router router) {
        router.route().handler(BodyHandler.create().setDeleteUploadedFilesOnEnd(true).setBodyLimit(1000000L));
    }


    /**
     * 记录HTTP请求的日志
     *
     * @param router
     */
    public void requestlog(Router router) {
        router.route("/*").handler(LoggerHandler.create(LoggerFormat.DEFAULT));//请求接口日志
    }

    /**
     * 配置静态资源路径
     *
     * @param router
     */
    public void staticResource(Router router) {
        router.route("/*").handler(StaticHandler.create().setWebRoot("static")
                .setCachingEnabled(true).setDirectoryListing(false));
    }

}
