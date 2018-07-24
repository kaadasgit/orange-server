package cn.orangeiot.managent.verify;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.Map;
import java.util.Objects;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-12-08
 */
public class VerifyParamsUtil {

    private static Logger logger = LogManager.getLogger(VerifyParamsUtil.class);

    /**
     * 检验参数是否正确
     */
    @SuppressWarnings("Duplicates")
    public static void verifyParams(RoutingContext routingContext, JsonObject params, Handler<AsyncResult<JsonObject>> handler) {
        logger.info("VerifyParamsUtil==verifyParams==params = " + params);
        if (null != params && params.size() > 0 && Objects.nonNull(routingContext.get("params"))) {
            Boolean isFlag = true;
            JsonObject jsonObject = new JsonObject(routingContext.get("params").toString());
            Map<String, Object> map = params.getMap();//
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                try {
                    Class<?> clazz = Class.forName(entry.getValue().toString());
                    if (Objects.nonNull(jsonObject.getValue(entry.getKey())) && clazz.isInstance(jsonObject.getValue(entry.getKey()))) {
                        if (jsonObject.getValue(entry.getKey()) instanceof String
                                && jsonObject.getValue(entry.getKey()).toString().trim().length() <= 0) {
                            isFlag = false;//参数类型校验失败
                            break;
                        }
                    } else {
                        isFlag = false;//参数类型校验失败
                        break;
                    }
                } catch (Exception e) {
                    logger.info("VerifyParamsUtil==verifyParams==params cast type is Fail");
                    handler.handle(Future.failedFuture(e));
                }
            }
            if (isFlag) {
                handler.handle(Future.succeededFuture(jsonObject));
            } else {
                logger.info("VerifyParamsUtil==verifyParams==params type is Fail");
                handler.handle(Future.failedFuture("VerifyParamsUtil==verifyParams==params type is Fail"));
            }
        } else {
            logger.info("VerifyParamsUtil==verifyParams==params is null");
            handler.handle(Future.failedFuture("VerifyParamsUtil==verifyParams==params is null"));
        }
    }

}