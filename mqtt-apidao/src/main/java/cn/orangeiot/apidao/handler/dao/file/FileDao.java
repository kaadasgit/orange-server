package cn.orangeiot.apidao.handler.dao.file;

import cn.orangeiot.apidao.client.MongoClient;
import cn.orangeiot.common.constant.mongodb.KdsUserHead;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.Objects;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-12-13
 */
public class FileDao {

    private static Logger logger = LogManager.getLogger(FileDao.class);


    /**
     * @Description 获取头像
     * @author zhang bo
     * @date 17-12-13
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void onGetHeaderImg(Message<JsonObject> message) {
        MongoClient.client.findOne(KdsUserHead.COLLECT_NAME, message.body(), new JsonObject().put(KdsUserHead.CONTENT, "")
                .put(KdsUserHead._ID, 0).put(KdsUserHead.SIZE, "").put(KdsUserHead.CONTENT_TYPE, "").put(KdsUserHead.UPLOAD_DATE, ""), res -> {
            if (res.failed()) {
                logger.error(res.cause().getMessage(), res.cause());
            } else {
                if (Objects.nonNull(res.result())) {
                    message.reply(res.result());
                } else {
                    message.reply(null);
                }
            }
        });
    }


    /**
     * @Description 上传头像
     * @author zhang bo
     * @date 17-12-13
     * @version 1.0
     */
    public void onUploadHeaderImg(Message<JsonObject> message) {
        MongoClient.client.removeDocument(KdsUserHead.COLLECT_NAME,new JsonObject().put(KdsUserHead.UID, message.body().getString("uid")),rs->{
            if (rs.failed()) {
                logger.error(rs.cause().getMessage(), rs.cause());
            }else{
                MongoClient.client.save(KdsUserHead.COLLECT_NAME,
                        message.body(), res -> {
                            if (res.failed()) {
                                logger.error(res.cause().getMessage(), res.cause());
                            } else if (Objects.nonNull(res.result())) {
                                message.reply(new JsonObject());
                            } else {
                                message.reply(null);
                            }
                        });
            }

        });
    }


}
