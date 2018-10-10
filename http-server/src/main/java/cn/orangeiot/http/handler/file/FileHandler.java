package cn.orangeiot.http.handler.file;

import cn.orangeiot.common.constant.HttpAttrType;
import cn.orangeiot.common.genera.ErrorType;
import cn.orangeiot.common.genera.Result;
import cn.orangeiot.common.options.SendOptions;
import cn.orangeiot.common.utils.StatusCode;
import cn.orangeiot.reg.EventbusAddr;
import cn.orangeiot.reg.file.FileAddr;
import cn.orangeiot.reg.user.UserAddr;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-12-13
 */
public class FileHandler implements EventbusAddr {

    private static Logger logger = LogManager.getLogger(FileHandler.class);


    private Vertx vertx;

    private JsonObject config;

    public FileHandler(Vertx vertx, JsonObject config) {
        this.vertx = vertx;
        this.config = config;
    }


    /**
     * @Description 获取用户头像
     * @author zhang bo
     * @date 17-12-13
     * @version 1.0
     */
    public void getHeaderImg(RoutingContext routingContext) {
        if (Objects.nonNull(routingContext.request().getParam("uid"))) {
            isToken(routingContext, ars -> {
                if (ars.failed()) {
                    routingContext.fail(500);
                } else {
                    String uid = routingContext.request().getParam("uid");
                    if (StringUtils.isNotBlank(routingContext.get("uid")))
                        uid = routingContext.get("uid").toString();

                    vertx.eventBus().send(FileAddr.class.getName() + GET_FILE_HEADER, new JsonObject().put("uid"
                            , uid), SendOptions.getInstance(), (AsyncResult<Message<JsonObject>> rs) -> {
                        if (rs.failed()) {
                            routingContext.fail(501);
                        } else {
                            if (Objects.nonNull(rs.result().body())) {
                                logger.info("get Images params : uid -> {} , return fileSize -> {}", routingContext.request().getParam("uid")
                                        , rs.result().body().getLong("size"));

                                routingContext.response().setChunked(true).putHeader("content-Length", String.valueOf(rs.result().body().getLong("size")))
                                        .putHeader("Cache-Control", "no-store").putHeader("content-Type", rs.result().body().getString("contentType"))
                                        .putHeader("Pragma", "no-cache").putHeader("Expires", "0")
                                        .write(Buffer.buffer(rs.result().body().getJsonObject("content").getBinary("$binary"))).end();
                            } else {
                                logger.info("get Images params : uid -> {} , return fileSize -> 0", routingContext.request().getParam("uid"));
                                routingContext.response().setStatusCode(StatusCode.Not_FOUND).end();
                            }

                        }
                    });
                }
            });
        }
    }


    /**
     * @Description 上传头像
     * @author zhang bo
     * @date 17-12-13
     * @version 1.0
     */
    public void downHeaderImg(RoutingContext routingContext) {
        isToken(routingContext, ars -> {
            if (ars.failed()) {
                routingContext.fail(500);
            } else {
                if (ars.result() && Objects.nonNull(routingContext.request().formAttributes().get("uid"))
                        && routingContext.fileUploads().size() == 1) {

                    if (StringUtils.isNotBlank(routingContext.get("uid")))
                        routingContext.request().formAttributes().set("uid", routingContext.get("uid").toString());

                    //文件数据
                    for (FileUpload f : routingContext.fileUploads()) {
                        logger.debug("upload Images params : uid -> {} , fileSize -> {} bytes", routingContext.request().formAttributes().get("uid")
                                , f.size());
                        Buffer fileByteBuffer = vertx.fileSystem().readFileBlocking(f.uploadedFileName());
                        vertx.eventBus().send(FileAddr.class.getName() + UPLOAD_HEADER_IMG, new JsonObject().put("name", f.fileName())
                                        .put("contentType", f.contentType()).put("size", f.size()).put("content",
                                        new JsonObject().put("$binary", fileByteBuffer.getBytes())).put("uid",
                                        routingContext.request().formAttributes().get("uid")).put("uploadDate", new JsonObject().put("$date",
                                        OffsetDateTime.of(LocalDateTime.now(), ZoneOffset.of("+08:00")).toString())), SendOptions.getInstance()
                                , (AsyncResult<Message<String>> rs) -> {
                                    if (rs.failed()) {
                                        routingContext.fail(501);
                                    } else {
                                        if (Objects.nonNull(rs.result())) {
                                            routingContext.response().end(JsonObject.mapFrom(new Result<String>()).toString());
                                        } else {
                                            routingContext.response().end(JsonObject.mapFrom(new Result<String>().setErrorMessage(
                                                    ErrorType.UPLOAD_FILE_FAIL.getKey(), ErrorType.UPLOAD_FILE_FAIL.getValue()
                                            )).toString());
                                        }
                                    }
                                });
                    }
                } else {
                    logger.warn("upload Images params fail: uid -> {} , filecount -> {}"
                            , routingContext.request().formAttributes().get("uid"), routingContext.fileUploads().size());
                    routingContext.response().setStatusCode(StatusCode.UNAUTHORIZED);
                    routingContext.response().end(JsonObject.mapFrom(new Result<String>().setErrorMessage(
                            ErrorType.RESULT_LOGIN_NO.getKey(), ErrorType.RESULT_LOGIN_NO.getValue()
                    )).toString());
                }
            }
        });
    }


    /**
     * 检查是否有token
     *
     * @return
     */
    public void isToken(RoutingContext routingContext, Handler<AsyncResult<Boolean>> handler) {
        String token = routingContext.request().getHeader("token");
        if (StringUtils.isNotBlank(token)) {
            vertx.eventBus().send(UserAddr.class.getName() + VERIFY_LOGIN, token, SendOptions.getInstance()
                    , (AsyncResult<Message<Boolean>> rs) -> {
                        if (rs.failed()) {
                            handler.handle(Future.failedFuture(rs.cause()));
                        } else {
                            routingContext.put("uid", rs.result().headers().get("uid"));
                            handler.handle(Future.succeededFuture(rs.result().body()));
                        }
                    });
        } else {
            logger.debug("Images checked token is null");
            routingContext.response().setStatusCode(StatusCode.UNAUTHORIZED);
            routingContext.response().putHeader(HttpAttrType.CONTENT_TYPE_JSON.getKey()
                    , HttpAttrType.CONTENT_TYPE_JSON.getValue()).end(JsonObject.mapFrom(new Result<String>().setErrorMessage(
                    ErrorType.RESULT_LOGIN_NO.getKey(), ErrorType.RESULT_LOGIN_NO.getValue()
            )).toString());
        }

    }

}
