package cn.orangeiot.http.handler;

import cn.orangeiot.common.constant.HttpAttrType;
import cn.orangeiot.common.genera.ErrorType;
import cn.orangeiot.common.genera.Result;
import cn.orangeiot.reg.user.UserAddr;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-12-08
 */
public class BaseHandler implements UserAddr {

    private Set<String> allowHeaders = new HashSet<>();//cors跨域header

    private Set<HttpMethod> allowMethods = new HashSet<>();//请求方法

    private List<String> excludePathList;//过滤排除的url路径集合

    private static Logger logger = LogManager.getLogger(BaseHandler.class);

    private Vertx vertx;

    private JsonObject conf;

    public BaseHandler(Vertx vertx, JsonObject conf) {
        this.vertx = vertx;
        this.conf = conf;
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
     * 配置静态资源路径
     *
     * @param router
     */
    public void staticResource(Router router) {
        router.route("/static/*").handler(StaticHandler.create().setWebRoot("static")
                .setCachingEnabled(false).setDirectoryListing(false));
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
        }).handler(TimeoutHandler.create(2000, 509));//超时处理,返回错误码;

    }


    /**
     * 配置content-type返回类型
     */
    public void produces(Router router) {
        router.route("/*").handler(ResponseContentTypeHandler.create());
    }


    /**
     * 全局的编码转义
     *
     * @param router
     */
    public void globalIntercept(Router router) {
        router.post("/*").handler(routingContext -> {
            logger.info("/*===globalIntercept params -> " + routingContext.getBodyAsString());
//            String params = EscapeUtils.escapeHtml(routingContext.getBodyAsString());//对特殊字符转义
            String params = routingContext.getBodyAsString().replace("\\s", "").replace("\n", "");
            if (this.paramsIsJsonObject(routingContext, params)) {
                if (!excludePathList.contains(routingContext.request().uri()))
                    isLogin(routingContext);
                else
                    routingContext.next();
            } else {
                routingContext.fail(806);//非法参数
            }
        }).consumes(HttpAttrType.CONTENT_TYPE_JSON.getValue());

    }


    /**
     * 用戶是否登錄
     *
     * @return
     */
    public void isLogin(RoutingContext routingContext) {
        Result<Object> result = new Result<>();
        String token = routingContext.request().getHeader("token");
        validationToken(token, res -> {
            if (res.failed()) {
                routingContext.fail(res.cause());
            } else if (res.result()) {
                routingContext.next();
            } else {
                result.setErrorMessage(ErrorType.RESULT_LOGIN_NO.getKey(), ErrorType.RESULT_LOGIN_NO.getValue());
                routingContext.response().putHeader(HttpAttrType.CONTENT_TYPE_JSON.getKey(), HttpAttrType.CONTENT_TYPE_JSON.getValue())
                        .end(JsonObject.mapFrom(result).toString());
            }
        }, routingContext);
    }


    /**
     * 验证token的有效性
     *
     * @param token
     */
    public void validationToken(String token, Handler<AsyncResult<Boolean>> handler, RoutingContext routingContext) {
        if (StringUtils.isNotBlank(token)) {
            vertx.eventBus().send(UserAddr.class.getName() + VERIFY_LOGIN, token, (AsyncResult<Message<Boolean>> rs) -> {
                if (rs.failed()) {
                    handler.handle(Future.failedFuture(rs.cause()));
                } else {
                    if (!rs.result().headers().isEmpty())
                        routingContext.put("uid", rs.result().headers().get("uid"));
                    handler.handle(Future.succeededFuture(rs.result().body()));
                }
            });
        } else {
            handler.handle(Future.succeededFuture(false));
        }
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
     * 加载过滤排除的url路径集合
     */
    public void loadFilterUrl() {
        InputStream excludePathIn = BaseHandler.class.getResourceAsStream("/filterPath.json");
        String excludePathConf = "";//jdbc连接配置
        try {
            excludePathConf = IOUtils.toString(excludePathIn, "UTF-8");//获取配置

            if (!excludePathConf.equals("")) {
                JsonObject json = new JsonObject(excludePathConf);
                excludePathList = json.getJsonArray("exclude_path").getList();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
