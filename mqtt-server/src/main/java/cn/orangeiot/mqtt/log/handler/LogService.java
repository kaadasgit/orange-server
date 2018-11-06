package cn.orangeiot.mqtt.log.handler;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-10-22
 */
public interface LogService {

    /**
     * @param msgId   消息id
     * @param payload 负荷
     * @param timerId 重發器id
     * @param qos     mqtt topic qos質量級別
     * @Description 写入log
     * @author zhang bo
     * @date 18-10-22
     * @version 1.0
     */
    void writeLog(String payload, int msgId, long timerId, byte qos, Handler<AsyncResult<Boolean>> handler);


    /**
     * @Description 創建主題
     * @author zhang bo
     * @date 18-10-23
     * @version 1.0
     */
    void createPartitionLog(Handler<AsyncResult<Boolean>> handler);


    /**
     * @param msgId 消息id,唯一標識
     * @Description 消費log
     * @author zhang bo
     * @date 18-10-22
     * @version 1.0
     */
    void consumLog(int msgId, Handler<AsyncResult<Long>> handler);


    /**
     * @Description 处理离线消息
     * @author zhang bo
     * @date 18-10-22
     * @version 1.0
     */
    void processOfflineMsg();


    /**
     * @Description 關閉狀態
     * @author zhang bo
     * @date 18-10-30
     * @version 1.0
     */
    void closeState();


    /**
     * @Description 更新定時器id
     * @author zhang bo
     * @date 18-10-30
     * @version 1.0
     */
    void updateTimerId(int msgId, long timerId, Handler<AsyncResult<Boolean>> handler);


    /**
     * @Description 釋放資源
     * @author zhang bo
     * @date 18-11-2
     * @version 1.0
     */
    void release();


}
