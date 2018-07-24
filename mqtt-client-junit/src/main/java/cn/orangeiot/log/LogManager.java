package cn.orangeiot.log;

import cn.orangeiot.common.utils.UUIDUtils;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-07-01
 */
public class LogManager {


    private static Logger logger = org.apache.logging.log4j.LogManager.getLogger(LogManager.class);

    private Vertx vertx;

    private String dir;

    private final String SUFFIX = ".json";

    private final String REGEX = ".*\\.json";

    public LogManager(Vertx vertx, String dir) {
        this.vertx = vertx;
        this.dir = dir;
    }

    /**
     * @Description 更新接口数据
     * @author zhang bo
     * @date 18-7-1
     * @version 1.0
     */
    public void updateLog(String title, JsonObject msg, Handler<AsyncResult<Boolean>> handler) {
        String filePath = dir + "/" + msg.getString("scheme") + "/" + title + SUFFIX;//目标文件
        String schemePath = dir + "/" + msg.getString("scheme");//主题目录
        vertx.fileSystem().exists(schemePath, mkRes -> {
            if (mkRes.succeeded() && mkRes.result()) {
                createDoc(filePath, msg, handler);
            } else {
                vertx.fileSystem().mkdir(schemePath, mkArs -> {
                    if (mkArs.succeeded()) {
                        createDoc(filePath, msg, handler);
                    } else {
                        logger.error(mkArs.cause().getMessage(), mkArs);
                        handler.handle(Future.succeededFuture(false));
                    }
                });
            }
        });
    }


    /**
     * @Description 创建文档
     * @author zhang bo
     * @date 18-7-9
     * @version 1.0
     */
    public void createDoc(String filePath, JsonObject msg, Handler<AsyncResult<Boolean>> handler) {
        vertx.fileSystem().exists(filePath, result -> {
            if (result.succeeded() && result.result()) {//文件是否存在
                //更新log
                vertx.fileSystem().open(filePath, new OpenOptions().setCreateNew(false).setCreate(false).setRead(true)
                        .setAppend(true), rs -> {
                    if (rs.failed()) {
                        logger.error(rs.cause().getMessage(), rs);
                        handler.handle(Future.succeededFuture(false));
                    } else {
                        rs.result().setReadBufferSize(100000);//讀取緩衝區的大小
                        rs.result().handler(buffer -> {
//                            JsonArray jsonArray = new JsonArray(buffer).add(new JsonObject().put("topic", msg.getString("topic"))
                            JsonArray jsonArray = new JsonArray().add(new JsonObject().put("topic", msg.getString("topic"))
                                    .put("payload", msg.getJsonObject("payload"))
                                    .put("qos", msg.getInteger("qos"))
                                    .put("description", Objects.nonNull(msg.getValue("description")) ? msg.getString("description") : "")
                                    .put("id", UUIDUtils.getUUID()));
                            rs.result().write(Buffer.buffer(jsonArray.encodePrettily().toString().getBytes().length)
                                    .appendString(jsonArray.encodePrettily().toString()), 0L, as -> {
                                if (as.succeeded()) {
                                    handler.handle(Future.succeededFuture(true));
                                } else {
                                    logger.error(as.cause().getMessage(), as);
                                    handler.handle(Future.succeededFuture(false));
                                }
                                rs.result().close();
                            });
                        });
                    }
                });
            } else {
                JsonArray jsonArray = new JsonArray().add(new JsonObject().put("topic", msg.getString("topic"))
                        .put("payload", msg.getJsonObject("payload")).put("qos", msg.getInteger("qos"))
                        .put("description", Objects.nonNull(msg.getValue("description")) ? msg.getString("description") : "")
                        .put("id", UUIDUtils.getUUID()));
                vertx.fileSystem().createFile(filePath, ars -> {
                    if (ars.failed()) {
                        logger.error(ars.cause().getMessage(), ars);
                        handler.handle(Future.succeededFuture(false));
                    } else {
                        vertx.fileSystem().writeFile(filePath, Buffer.buffer(jsonArray.encodePrettily().toString().getBytes().length)
                                .appendString(jsonArray.encodePrettily().toString()), rs -> {
                            if (rs.succeeded()) {
                                handler.handle(Future.succeededFuture(true));
                            } else {
                                logger.error(rs.cause().getMessage(), rs);
                                handler.handle(Future.succeededFuture(false));
                            }
                        });
                    }
                });
            }
        });
    }


    /**
     * @Description 獲取所有標題
     * @author zhang bo
     * @date 18-7-3
     * @version 1.0
     */
    public void getDirTitle(Handler<AsyncResult<List>> handler) {
        vertx.fileSystem().readDir(dir, rs -> {
            if (rs.failed()) {
                logger.error(rs.cause().getMessage(), rs.cause());
                handler.handle(Future.succeededFuture());
            } else {
                List<JsonObject> lists = new ArrayList<>();
                List<String> dirList = rs.result().stream().map(e ->
                        e.substring(e.lastIndexOf("/") + 1)).filter(e -> e.indexOf(".") < 0).collect(Collectors.toList());
                AtomicInteger atomicInteger = new AtomicInteger(0);
                int times = dirList.size();
                vertx.executeBlocking((Future<List<JsonObject>> future) -> {
                    dirList.forEach(res ->
                            vertx.fileSystem().readDir(dir + "/" + res, REGEX, ars -> {
                                if (ars.failed()) {
                                    logger.error(ars.cause().getMessage(), ars.cause());
                                    handler.handle(Future.succeededFuture());
                                } else {
                                    List<String> titles = ars.result().stream().map(e ->
                                            e.substring(e.lastIndexOf("/") + 1, e.indexOf("."))).collect(Collectors.toList());
                                    lists.add(new JsonObject().put(res, new JsonArray(titles)));
                                    atomicInteger.incrementAndGet();
                                    if (atomicInteger.get() == times) {
                                        future.complete(lists);
                                    }
                                }
                            }));
                }, futureRes -> handler.handle(Future.succeededFuture(futureRes.result())));
            }
        });
    }


    /**
     * @Description 获取api文档
     * @author zhang bo
     * @date 18-7-3
     * @version 1.0
     */
    public void getApiDoc(String scheme, String rootPath, Handler<AsyncResult<JsonArray>> handler) {
        vertx.fileSystem().readFile(dir + "/" + rootPath + "/" + scheme + SUFFIX, rs -> {
            if (rs.failed()) {
                logger.error(rs.cause().getMessage(), rs.cause());
                handler.handle(Future.succeededFuture());
            } else {
                handler.handle(Future.succeededFuture(new JsonArray(rs.result())));
            }
        });
    }
}
