package cn.orangeiot.mqtt.util;

import cn.orangeiot.mqtt.MQTTSession;
import cn.orangeiot.mqtt.log.handler.LogService;
import cn.orangeiot.mqtt.log.handler.impl.LogServiceImpl;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.googlecode.concurrentlinkedhashmap.Weighers;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sun.dc.pr.PRError;

import java.util.Map;


/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-10-25
 */
public class LogFileUtils {

    private static Logger logger = LogManager.getLogger(LogFileUtils.class);

    ConcurrentLinkedHashMap<String, LogService> map = new ConcurrentLinkedHashMap.Builder<String, LogService>()
            .maximumWeightedCapacity(10000).weigher(Weighers.singleton()).build();//LRU 存储离线打log集合

    private final String dirPath;

    private final int segmentSize;

    private Vertx vertx;

    private final long DEFAULT_TIMERID = 0L;

    private final long liveTimeStamp;

    /**
     * @param dirPath     數據存放目錄
     * @param vertx       实例
     * @param segmentSize log 文件最大值,超過重新生成log
     * @Description init 初始化
     * @author zhang bo
     * @date 18-10-25
     * @version 1.0
     */
    public LogFileUtils(String dirPath, Vertx vertx, int segmentSize, long liveTimeStamp) {
        this.dirPath = dirPath;
        this.segmentSize = segmentSize;
        this.vertx = vertx;
        this.liveTimeStamp = liveTimeStamp;
    }

    /**
     * @Description 移除實例
     * @author zhang bo
     * @date 18-10-30
     * @version 1.0
     */
    public void remove(String clientId) {
        if (clientId != null) {
            LogService logService;
            if ((logService = map.get(clientId)) != null) {
                logService.release();
            }
            map.remove(clientId);
        }
    }

    /**
     * @Description 移除實例
     * @author zhang bo
     * @date 18-11-5
     * @version 1.0
     */
    public void remove(String clientId, LogService logService) {
        if (clientId != null) {
            if (logService != null) {
                logService.release();
            }
            map.remove(clientId, logService);
        }
    }


    /**
     * @Description 获取或创建实例
     * @author zhang bo
     * @date 18-10-25
     * @version 1.0
     */
    private synchronized void getOrCreateContext(String clientId, Handler<AsyncResult<LogService>> handler) {
        LogService logService = new LogServiceImpl(dirPath, vertx, clientId, segmentSize, liveTimeStamp);
        logService.createPartitionLog(res -> {
            if (res.failed()) {
                handler.handle(Future.failedFuture(res.cause()));
            } else {
                if (res.result()) {
                    handler.handle(Future.succeededFuture(logService));
                } else {//获取创建失败
                    handler.handle(Future.failedFuture("getOrCreateContext is fail"));
                }
            }
        });
    }

    /**
     * @Description 写入离线log
     * @author zhang bo
     * @date 18-10-25
     * @version 1.0
     */
    public void writeOfflineLog(String clientId, int msgId, String payLoad, byte qos) {
        logger.debug("write OfflineLog params , clientId -> {} , msgId -> {} , qos -> {}", clientId, msgId, qos);
        LogService logService = map.get(clientId);
        if (logService != null) {//存在
            logService.writeLog(payLoad, msgId, DEFAULT_TIMERID, qos, ars -> {
                if (ars.failed()) {
                    logger.error(ars.cause().getMessage(), ars);
                }
            });//写入log
        } else {
            getOrCreateContext(clientId, res -> {
                if (res.failed()) {
                    logger.error(res.cause().getMessage(), res);
                } else {
                    map.put(clientId, res.result());
                    res.result().writeLog(payLoad, msgId, DEFAULT_TIMERID, qos, ars -> {
                        if (ars.failed()) {
                            logger.error(ars.cause().getMessage(), ars);
                        }
                    });//写入log
                }
            });
        }

    }

}
