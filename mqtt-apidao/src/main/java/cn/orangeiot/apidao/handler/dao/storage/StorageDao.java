package cn.orangeiot.apidao.handler.dao.storage;

import cn.orangeiot.apidao.client.StorageClient;
import cn.orangeiot.reg.message.MessageAddr;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import jetbrains.exodus.bindings.LongBinding;
import jetbrains.exodus.bindings.StringBinding;
import jetbrains.exodus.env.Cursor;
import jetbrains.exodus.env.Store;
import jetbrains.exodus.env.StoreConfig;
import jetbrains.exodus.env.Transaction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-05-16
 */
public class StorageDao implements MessageAddr {


    private static Logger logger = LogManager.getLogger(StorageDao.class);

    private Vertx vertx;

    /**
     * @Description 存储服务
     * @author zhang bo
     * @date 18-5-17
     * @version 1.0
     */
    public StorageDao(Vertx vertx) {
        this.vertx = vertx;
    }

    /**
     * @Description 存储数据
     * @author zhang bo
     * @date 18-3-8
     * @version 1.0
     */
    public void putStorageData(Message<JsonObject> message) {
        logger.info("params -> {}", message.body());
        if (Objects.nonNull(message.body()) && !message.headers().isEmpty()) {
            StorageClient.getEnv().executeInTransaction((@NotNull final Transaction txn) -> {
                final Store store = StorageClient.getEnv().openStore(message.headers().get("clientId")
                        , StoreConfig.WITH_DUPLICATES, txn);
                store.put(txn, StringBinding.stringToEntry(message.headers().get("msgId"))
                        , StringBinding.stringToEntry(message.body().toString()));
            });
        }
    }


    /**
     * @Description 移除存储数据
     * @author zhang bo
     * @date 18-3-8
     * @version 1.0
     */
    public void delStorageData(Message<JsonObject> message) {
        logger.info("params -> {}", message.body());
        if (!message.headers().isEmpty()) {
            StorageClient.getEnv().executeInTransaction((@NotNull final Transaction txn) -> {
                final Store store = StorageClient.getEnv().openStore(message.headers().get("clientId")
                        , StoreConfig.WITH_DUPLICATES, txn);
                store.delete(txn, StringBinding.stringToEntry(message.headers().get("msgId")));
            });
        }
    }


    /**
     * @Description 移除所有存储数据
     * @author zhang bo
     * @date 18-3-8
     * @version 1.0
     */
    public void delAllStorageData(Message<JsonObject> message) {
        logger.info("params -> {}", message.body());
        if (Objects.nonNull(message.body()) && !message.headers().isEmpty()) {
            StorageClient.getEnv().executeInTransaction((@NotNull final Transaction txn) -> {
                StorageClient.getEnv().removeStore(message.headers().get("clientId"), txn);
            });
        }
    }


    /**
     * @Description 獲取存储数据
     * @author zhang bo
     * @date 18-3-8
     * @version 1.0
     */
    public void getStorageData(Message<JsonObject> message) {
        logger.info("params -> {}", message.body());
        if (Objects.nonNull(message.headers().get("clientId"))) {
            StorageClient.getEnv().executeInTransaction((@NotNull final Transaction txn) -> {
                final Store store = StorageClient.getEnv().openStore(message.headers().get("clientId")
                        , StoreConfig.WITH_DUPLICATES, txn);
                Cursor cursor = store.openCursor(txn);

                while (cursor.getNext()) {
                    logger.info("================" + StringBinding.entryToString(cursor.getValue()));
                    String payload = StringBinding.entryToString(cursor.getValue());
                    JsonObject jsonObject = new JsonObject(payload);
                    vertx.eventBus().send(MessageAddr.class.getName() + SEND_STORAGE_MSG,
                            new JsonObject(jsonObject.getString("message")), new DeliveryOptions().addHeader("msgId"
                                    , StringBinding.entryToString(cursor.getKey()))
                                    .addHeader("topic", jsonObject.getString("topic"))
                                    .addHeader("qos", jsonObject.getString("qos"))
                                    .addHeader("uid", message.headers().get("clientId")));
                }
            });
        }
    }


}
