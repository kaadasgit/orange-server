package cn.orangeiot.apidao.handler.dao.file;

import cn.orangeiot.apidao.client.MongoClient;
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
        MongoClient.client.findOne("kdsUserHead", message.body(), new JsonObject().put("content", "")
                .put("_id", 0).put("size", "").put("contentType", "").put("uploadDate", ""), res -> {
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
        MongoClient.client.removeDocument("kdsUserHead",new JsonObject().put("uid", message.body().getString("uid")),rs->{
            if (rs.failed()) {
                logger.error(rs.cause().getMessage(), rs.cause());
            }else{
                MongoClient.client.save("kdsUserHead",
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
