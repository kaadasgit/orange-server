package cn.orangeiot.mqtt.log.timer;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.file.OpenOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author zhang bo
 * @version 1.0
 * @Description 清除过期的log
 * @date 2018-11-01
 */
public class CleanOverExpireTimer {

    private Vertx vertx;

    private long time;//周期定時時間,milliseconds

    private long count;//一批次的数量

    private String DIR_PATH;//主目录

    private long EXPIRE_TIME;//有效时间,milliseconds

    private final String REGEX = ".*\\.log";

    private FileSystem fileSystem;

    private final String RECORD_LOG = "mqttRecord.record";//記錄文件

    private static Logger logger = LogManager.getLogger(CleanOverExpireTimer.class);

    /**
     * @param vertx      对象实例
     * @param time       周期时间,单位毫秒
     * @param dirPath    主目录
     * @param expireTime 过期时间,单位毫秒
     * @Description
     * @author zhang bo
     * @date 18-11-1
     * @version 1.0
     */
    public CleanOverExpireTimer(Vertx vertx, long time, String dirPath, long expireTime) {
        this.DIR_PATH = dirPath;
        this.EXPIRE_TIME = expireTime;
        this.vertx = vertx;
        this.time = time;
        this.fileSystem = vertx.fileSystem();
        init();
    }


    /**
     * @Description 初始化
     * @author zhang bo
     * @date 18-11-1
     * @version 1.0
     */
    public void init() {
        vertx.executeBlocking(future -> {
            vertx.setPeriodic(time, timeId -> {
                cleanLog(timeId, future);
            });
        }, true, null);
    }


    /**
     * @Description 獲取當前目錄所有log文件
     * @author zhang bo
     * @date 18-11-1
     * @version 1.0
     */
    public File[] getDirAllLogFile(String path) {
        File file = new File(path);
        return file.listFiles((File dir, String name) -> Pattern.matches(REGEX, name));
    }


    /**
     * @Description 獲取文件有效時間
     * @author zhang bo
     * @date 18-11-1
     * @version 1.0
     */
    public long getFileExpireTime(String fileName) {
        String[] arrs = fileName.split("-");
        if (arrs.length >= 3) {
            return Long.parseLong(arrs[2].substring(0, arrs[2].length() - 4));
        } else {
            return 0;
        }
    }


    /**
     * @Description 獲取記錄次數
     * @author zhang bo
     * @date 18-11-1
     * @version 1.0
     */
    private void getRecordTimes(Handler<AsyncResult<Integer>> handler) {
        fileSystem.open(DIR_PATH + "." + RECORD_LOG, new OpenOptions().setRead(true).setCreate(false).setCreateNew(false), res -> {
            if (res.failed()) {
                logger.error(res.cause().getMessage(), res);
                handler.handle(Future.succeededFuture(0));
            } else {
                res.result().read(Buffer.buffer(4), 0, 0, 4, rs -> {
                    if (rs.failed()) {
                        logger.error(rs.cause().getMessage(), rs);
                        handler.handle(Future.succeededFuture(0));
                    } else {
                        handler.handle(Future.succeededFuture(rs.result().getInt(0)));
                    }
                });
            }
        });
    }


    /**
     * @Description 寫入記錄
     * @times 次數
     * @author zhang bo
     * @date 18-11-1
     * @version 1.0
     */
    private void writeRecord(int times) {
        fileSystem.writeFileBlocking(DIR_PATH + "." + RECORD_LOG, Buffer.buffer(4).appendLong(times));
    }

    /**
     * @param timeId 定時器唯一標識
     * @Description 清除过期log
     * @author zhang bo
     * @date 18-11-1
     * @version 1.0
     */
    public void cleanLog(long timeId, Future future) {
        File file = new File(DIR_PATH);
        File[] resfile = file.listFiles((File pathname) -> pathname.isDirectory());
        List<File> files = Arrays.stream(resfile).sorted().collect(Collectors.toList());
        for (int i = 0; i < files.size(); i++) {
            Arrays.stream(getDirAllLogFile(files.get(i).getAbsolutePath())).forEach(e -> {
                long currentFileTime = getFileExpireTime(e.getName());
                logger.info("cleanlog filename -> {} , currentFileTime -> {}", e.getName(), currentFileTime);
                if (currentFileTime != 0 && System.currentTimeMillis() - currentFileTime > this.EXPIRE_TIME) {//無效 clean
                    new File(e.getAbsolutePath()).delete();
                }
            });
        }
    }

}
