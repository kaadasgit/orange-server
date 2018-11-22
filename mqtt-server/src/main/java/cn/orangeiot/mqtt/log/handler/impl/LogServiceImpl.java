package cn.orangeiot.mqtt.log.handler.impl;

import cn.orangeiot.mqtt.log.handler.LogService;
import cn.orangeiot.reg.message.MessageAddr;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.FileSystem;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.FileSystemException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-10-22
 */
public class LogServiceImpl implements LogService, MessageAddr {

    private static Logger logger = LogManager.getLogger(LogServiceImpl.class);

    AsyncFile indexFile;//index文件

    AsyncFile logFile;//log文件

    AsyncFile stateFile;//當前狀態文件

    private String DEFAULT_GATEWAY_TOPIC = "/orangeiot/gwId/call";//網關主題

    private String DEFAULT_USER_TOPIC = "/clientId/rpc/reply";//用戶主題

    private String logFileName;//log名稱

    private final String DIR_PATH;//文件目录

    private FileSystem fileSystem;//文件系统

    private String clientId;//库户端id

    private final long SEGMENT_SIZE;//segment  max  size

    private int currentSegmentposition;//当前文件 position

    private final String INDEX_SUFFIX = ".index";//index文件后缀

    private final String LOG_SUFFIX = ".log";//log文件后缀

    private final String STATE_SUFFIX = ".state";//状态文件后缀

    private final String REGEX = ".*\\.log";

    private final String INDEX_FILE_PATH;//索引文件目錄

    private final String CLIENT_DIR;//client 目錄

    private volatile short partition = 0;//当前分区数

    private final short CAPACITY = 29;//一個index容量單位

    private AtomicInteger count;//离线消息数量

    private Vertx vertx;

    private final int MAX_MSGID = 65535;//最大值

    private volatile boolean offlineConsumState;//離線消費狀態

    private final long liveTimeStamp;//有效时间

    private AsyncFile[] logsFileHandle;//所有log文件句柄數

    private final short MAX_PARTITION = 10;//最大的分区数

    private boolean close;


    /**
     * @param dirPath       數據存放目錄
     * @param vertx         实例
     * @param clientId      客戶端id
     * @param segmentSize   log 文件最大值,超過重新生成log
     * @param liveTimeStamp 消息最大的有效时间
     * @Description
     * @author zhang bo
     * @date 18-10-23
     * @version 1.0
     */
    public LogServiceImpl(String dirPath, Vertx vertx, String clientId, int segmentSize, long liveTimeStamp) {
        this.DIR_PATH = dirPath;
        this.vertx = vertx;
        this.fileSystem = vertx.fileSystem();
        this.clientId = clientId;
        this.SEGMENT_SIZE = segmentSize;
        this.INDEX_FILE_PATH = dirPath + clientId + "/" + clientId + INDEX_SUFFIX;
        this.CLIENT_DIR = dirPath + clientId;
        this.offlineConsumState = true;
        this.liveTimeStamp = liveTimeStamp;
    }

    /**
     * @Description 创建目录和文件
     * @author zhang bo
     * @date 18-10-22
     * @version 1.0
     */
    private void mkdirAndLogFile(Handler<AsyncResult<Boolean>> handler) {
        this.fileSystem.mkdir(CLIENT_DIR, rs -> {
            if (rs.failed()) {
                handler.handle(Future.failedFuture(rs.cause().getMessage()));
            } else {
                this.logFileName = clientId + "-0";//文件名稱

                String logFilePath = CLIENT_DIR + "/" + this.logFileName + LOG_SUFFIX;

                String STATEFilePath = CLIENT_DIR + "/" + clientId + STATE_SUFFIX;

                //創建成功,新建相應文件
                FileSystem currentIndexFile = this.fileSystem.createFileBlocking(INDEX_FILE_PATH);
                FileSystem currentLogFile = this.fileSystem.createFileBlocking(logFilePath);
                FileSystem stateFile = this.fileSystem.createFileBlocking(STATEFilePath);

                if (currentIndexFile != null && currentLogFile != null && stateFile != null) {
                    //open 文件句柄
                    if (this.indexFile == null && this.fileSystem != null)
                        this.indexFile = this.fileSystem.openBlocking(INDEX_FILE_PATH, new OpenOptions().setAppend(true).setRead(true).setWrite(true));
                    if (this.logFile == null && this.fileSystem != null)
                        this.logFile = this.fileSystem.openBlocking(logFilePath, new OpenOptions().setAppend(true).setRead(true).setWrite(true));
                    if (this.stateFile == null && this.fileSystem != null)
                        this.stateFile = this.fileSystem.openBlocking(STATEFilePath, new OpenOptions().setAppend(true).setRead(true).setWrite(true));

                    this.currentSegmentposition = 0;
                    this.partition = 0;
                    count = new AtomicInteger(0);
                    this.stateFile.write(Buffer.buffer(2).appendShort(this.partition), 4, res -> {
                        if (res.failed()) {
                            handler.handle(Future.failedFuture("mkdirAndLogFile write stateFile fail , " + rs.cause().getMessage()));
                        } else {
                            handler.handle(Future.succeededFuture(true));
                        }
                    });
                } else {
                    handler.handle(Future.succeededFuture(false));
                }
            }
        });
    }


    /**
     * @Description 加载当前clientid信息
     * @author zhang bo
     * @date 18-10-23
     * @version 1.0
     */
    private void loadCurrentInfo(Handler<AsyncResult<Boolean>> handler) {

        String stateLogFile = CLIENT_DIR + "/" + clientId + STATE_SUFFIX;

        int stateSize = (int) new File(DIR_PATH + "/" + clientId + "/" + clientId + STATE_SUFFIX).length();
        if (this.stateFile == null && this.fileSystem != null)
            this.stateFile = this.fileSystem.openBlocking(stateLogFile, new OpenOptions().setAppend(true).setRead(true).setWrite(true));
        logger.debug("loadCurrentInfo params , clientId -> {} , position -> {}", clientId, stateSize);

        if (this.stateFile != null) {
            if (stateSize >= 6) {
                stateFile.read(Buffer.buffer(4), 0, 2, 4, res -> {
                    if (res.failed()) {
                        handler.handle(Future.failedFuture(res.cause()));
                    } else {
                        this.partition = res.result().getShort(2);
                        count = new AtomicInteger(res.result().getShort(0) & 0x0FFFF);

                        String currentLogfile = clientId + "-" + this.partition + LOG_SUFFIX;
                        this.logFileName = currentLogfile.substring(0, currentLogfile.length() - 4);//获取名称

                        this.currentSegmentposition = (int) new File(DIR_PATH + "/" + clientId + "/" + currentLogfile).length();

                        if (this.indexFile == null && this.fileSystem != null)
                            this.indexFile = this.fileSystem.openBlocking(INDEX_FILE_PATH, new OpenOptions().setAppend(true).setRead(true).setWrite(true));
                        if (this.logFile == null && this.fileSystem != null)
                            this.logFile = this.fileSystem.openBlocking(CLIENT_DIR + "/" + currentLogfile, new OpenOptions().setAppend(true).setRead(true).setWrite(true));

                        logger.debug("Msgcount , count -> {} , partition -> {}", count.get(), this.partition);
                        handler.handle(Future.succeededFuture(true));
                    }
                });
            } else {
                this.partition = 0;
                count = new AtomicInteger(0);

                String currentLogfile = clientId + "-" + this.partition + LOG_SUFFIX;
                this.logFileName = currentLogfile.substring(0, currentLogfile.length() - 4);//获取名称

                this.currentSegmentposition = (int) new File(DIR_PATH + "/" + clientId + "/" + currentLogfile).length();

                if (this.indexFile == null && this.fileSystem != null)
                    this.indexFile = this.fileSystem.openBlocking(INDEX_FILE_PATH, new OpenOptions().setAppend(true).setRead(true).setWrite(true));
                if (this.logFile == null && this.fileSystem != null)
                    this.logFile = this.fileSystem.openBlocking(CLIENT_DIR + "/" + currentLogfile, new OpenOptions().setAppend(true).setRead(true).setWrite(true));

                handler.handle(Future.succeededFuture(true));
            }
        } else {
            handler.handle(Future.failedFuture("this stateFile is null , clientId -> " + this.clientId));
        }
    }


    /**
     * @Description 检查是否存在log
     * @author zhang bo
     * @date 18-10-22
     * @version 1.0
     */
    private void checkExistsOrCreateLog(Handler<AsyncResult<Boolean>> handler) {
        if (this.fileSystem != null)
            this.fileSystem.exists(CLIENT_DIR, rs -> {
                if (rs.failed()) {
                    handler.handle(Future.failedFuture(rs.cause().getMessage()));
                } else {
                    if (!rs.result()) {//不存在
                        mkdirAndLogFile(handler);
                    } else {
                        loadCurrentInfo(handler);
                    }
                }
            });
        else
            handler.handle(Future.failedFuture("fileSystem is null"));
    }


    /**
     * @Description 關閉狀態
     * @author zhang bo
     * @date 18-10-30
     * @version 1.0
     */
    @Override
    public void closeState() {
        offlineConsumState = false;
    }

    /**
     * @Description 訂閱topic
     * @author zhang bo
     * @date 18-10-22
     * @version 1.0
     */
    @Override
    public void createPartitionLog(Handler<AsyncResult<Boolean>> handler) {
        logger.debug("create Partition log , clientId -> {}", clientId);
        checkExistsOrCreateLog(handler);
    }


    /**
     * @Description 獲取一個 partition log 文件
     * @author zhang bo
     * @date 18-10-23
     * @version 1.0
     */
    private String getNewLogFile(short partition) {
        return clientId + "-" + partition;
    }


    /**
     * @Description 更新文件名称
     * @author zhang bo
     * @date 18-11-1
     * @version 1.0
     */
    private void updateLogFile(String filePath) {
        fileSystem.moveBlocking(CLIENT_DIR + "/" + filePath, CLIENT_DIR + "/" + logFileName + "-" + System.currentTimeMillis() + LOG_SUFFIX);
    }

    /**
     * @Description 重新均衡大小
     * @author zhang bo
     * @date 18-10-23
     * @version 1.0
     */
    private void resize() throws FileSystemException {
        this.partition = this.partition >= MAX_PARTITION - 1 ? 0 : ++this.partition;//取新的partition

        String logfile = getNewLogFile(partition);

        //新建相應文件
        String path = CLIENT_DIR + "/" + logfile + LOG_SUFFIX;

//        if (this.fileSystem.existsBlocking(path)) {//已经存在
//            this.fileSystem.deleteBlocking(path);
//        }

        FileSystem currentLogFile = this.fileSystem.createFileBlocking(path);

        if (currentLogFile != null) {
            //open 文件句柄
            if (this.logFile != null)
                this.logFile.close();//释放pre一个文件句柄
            this.logFile = this.fileSystem.openBlocking(path, new OpenOptions().setAppend(true).setRead(true).setWrite(true));

            updateLogFile(this.logFileName + LOG_SUFFIX);

            this.currentSegmentposition = 0;
            this.logFileName = logfile;

            //寫入記錄
            this.stateFile.write(Buffer.buffer(2).appendShort(this.partition), 4, res -> {
                if (res.failed())
                    logger.error("resize stateFile write fail , partition -> " + this.partition + "  , " + res.cause().getMessage(), res);
            });
        } else {
            throw new FileSystemException("create file is fail");
        }
    }


    /**
     * @Description 检查logfile文件最大值
     * @author zhang bo
     * @date 18-10-22
     * @version 1.0
     */
    private synchronized void checkLogFileSize(int length, Handler<AsyncResult<Boolean>> handler) {
        if (currentSegmentposition + length > SEGMENT_SIZE) {//超過segment大小
            try {
                resize();
                handler.handle(Future.succeededFuture(true));
            } catch (FileSystemException e) {
                handler.handle(Future.failedFuture(e.getCause()));
            }
        } else {
            handler.handle(Future.succeededFuture(true));
        }
    }


    /**
     * @Description 释放文件句柄
     * @author zhang bo
     * @date 18-11-2
     * @version 1.0
     */
    private void closeFileHandle() {
        //释放资源
        if (logsFileHandle != null && logsFileHandle.length > 0) {
            for (int i = 0; i < this.logsFileHandle.length; i++) {
                if (this.logsFileHandle[i] != null) {
                    this.logsFileHandle[i].close();
                }
            }
        }
        this.logsFileHandle = null;
        close = true;
    }

    /**
     * @Description 释放资源
     * @author zhang bo
     * @date 18-11-2
     * @version 1.0
     */
    @Override
    public synchronized void release() {
        if (this.logFile != null) {
            this.logFile.close();
            this.logFile = null;
        }
        if (this.indexFile != null) {
            this.indexFile.close();
            this.indexFile = null;
        }
        if (this.stateFile != null) {
            this.stateFile.close();
            this.indexFile = null;
        }
        fileSystem = null;
        vertx = null;
        closeFileHandle();
    }


    /**
     * @Description
     * @author zhang bo
     * @date 18-10-23
     * @version 1.0
     */
    private long getOffsetAddr(int msgId) {
        return msgId * CAPACITY - 1;
    }


    @SuppressWarnings("Duplicates")
    @Override
    public void writeLog(String payload, int msgId, long timerId, byte qos, Handler<AsyncResult<Boolean>> handler) {
        logger.debug("write log , clientId -> {} , msgId -> {} , payload -> {} , timerId -> {} , qps -> {} , partition -> {}", clientId, msgId, payload
                , timerId, qos, this.partition);
        byte[] bytes = payload.getBytes();
        checkLogFileSize(bytes.length, ars -> {
            if (ars.failed()) {
                handler.handle(Future.failedFuture(ars.cause()));
            } else {
                if (ars.result()) {
                    //append log
                    int startOffset = this.currentSegmentposition;
                    if (this.logFile != null)
                        this.logFile.write(Buffer.buffer(bytes), this.currentSegmentposition, res -> {
                            if (res.failed()) {
                                handler.handle(Future.failedFuture(res.cause()));
                            } else {
                                this.currentSegmentposition += bytes.length;
                                logger.debug("writelog result , clientId -> {} , startOffset -> {} , end Offset -> {} , msgId -> {} , position -> {} , partition -> {}"
                                        , clientId, startOffset, this.currentSegmentposition, msgId, getOffsetAddr(msgId), this.partition);
                                if (this.indexFile != null)
                                    this.indexFile.write(Buffer.buffer(CAPACITY).appendShort((short) msgId).appendInt(startOffset).appendInt(this.currentSegmentposition)
                                            .appendByte((byte) 0).appendLong(timerId).appendByte((byte) this.partition)
                                            .appendLong(System.currentTimeMillis()).appendByte(qos), getOffsetAddr(msgId), rs -> {
                                        if (rs.failed()) {
                                            handler.handle(Future.failedFuture(rs.cause()));
                                        } else {
                                            handler.handle(Future.succeededFuture(ars.result()));
                                            if (this.stateFile != null)
                                                this.stateFile.write(Buffer.buffer(4).appendShort((short) msgId).appendShort((short) (count.get() >= MAX_MSGID ? MAX_MSGID : count.incrementAndGet())), 0, writeRes -> {
                                                    if (writeRes.failed())
                                                        logger.error(writeRes.cause().getMessage(), "write stateFile fail , clientId -> " + clientId, writeRes);
                                                });
                                        }
                                    });
                                else
                                    handler.handle(Future.failedFuture("writeLog method this indexFile is null , clientId -> " + this.clientId));
                            }
                        });
                    else
                        handler.handle(Future.failedFuture("writeLog method this logFile is null , clientId -> " + this.clientId));
                } else {//fail
                    handler.handle(Future.succeededFuture(ars.result()));
                }
            }
        });
    }

    @Override
    public void consumLog(int msgId, Handler<AsyncResult<Long>> handler) {
        logger.debug("consum log , clientId -> {} , msgId -> {}", clientId, msgId);
        if (this.indexFile != null)
            this.indexFile.read(Buffer.buffer(9), 0, getOffsetAddr(msgId) + 10, 9, res -> {
                if (res.failed()) {
                    handler.handle(Future.failedFuture(res.cause()));
                } else {
                    byte valid = res.result().getByte(0);
                    long timerId = res.result().getLong(1);
                    if (valid == 0) {//有效消費,有可能是重發
                        if (this.indexFile != null)
                            this.indexFile.write(Buffer.buffer(1).appendByte((byte) 1), getOffsetAddr(msgId) + 10, rs -> {
                                if (rs.failed()) {
                                    handler.handle(Future.failedFuture(rs.cause()));
                                } else {
                                    handler.handle(Future.succeededFuture(timerId));
                                    logger.debug("consum log gettimerId , clientId -> {} , msgId -> {} , timerId -> {} , noconsumCout -> {}"
                                            , clientId, msgId, timerId, count.get());
                                    if (this.stateFile != null)
                                        this.stateFile.write(Buffer.buffer(4).appendShort((short) msgId).appendShort((short) (count.get() <= 0 ? 0 : count.decrementAndGet())), 0, writeRes -> {
                                            if (writeRes.failed())
                                                logger.error(writeRes.cause().getMessage(), "consum stateFile fail , clientId -> " + clientId, writeRes);
                                        });
                                }
                            });
                        else
                            handler.handle(Future.succeededFuture(0L));
                    } else {
                        handler.handle(Future.succeededFuture(timerId));
                    }
                }
            });
        else
            handler.handle(Future.succeededFuture(0L));
    }


    /**
     * @Description 獲取主題
     * @author zhang bo
     * @date 18-7-31
     * @version 1.0
     */
    private String getTopic() {
        String topic = "";
        String[] arrs = clientId.split(":");
        if (arrs[0].indexOf("app") >= 0) {
            topic = DEFAULT_USER_TOPIC.replace("clientId", arrs[1]);
        } else {
            topic = DEFAULT_GATEWAY_TOPIC.replace("gwId", arrs[1]);
        }
        return topic;
    }


    /**
     * @param msgId 消息id
     * @Description 清理离线消息数量
     * @author zhang bo
     * @date 18-10-26
     * @version 1.0
     */
    private void cleanOfflineCount(int msgId) {
        if (this.stateFile != null)
            stateFile.write(Buffer.buffer(4).appendShort((short) msgId).appendShort((short) 0), 0, res -> {
                if (res.failed()) {
                    logger.error(res.cause().getMessage(), res);
                }
            });
        closeFileHandle();
    }


    private void residurCall(AtomicInteger endIndex, int count, int msgId, int max) {
        long newPosition = getPrePoition(msgId);
        if (newPosition <= 0) {
            cleanOfflineCount(msgId);
            return;
        }
        int tempId = msgId;
        int newMax = max;
        tempId--;
        newMax--;
        if (tempId == 0)
            tempId = MAX_MSGID;
        consumResidueMsg(newPosition, endIndex, count, tempId, newMax);
    }


    /**
     * @Description 获取上一个msg的位置
     * @author zhang bo
     * @date 18-10-26
     * @version 1.0
     */

    private long getPrePoition(int msgId) {
        if (msgId == 0)
            msgId = MAX_MSGID;
        long position = msgId * CAPACITY - 1;
        return position;
    }

    /**
     * @Description 獲取開關狀態
     * @author zhang bo
     * @date 18-10-30
     * @version 1.0
     */
    public boolean isOfflineConsumState() {
        return offlineConsumState;
    }


    /**
     * @Description 处理判断
     * @author zhang bo
     * @date 18-11-1
     * @version 1.0
     */
    private void processFlag(AtomicInteger endIndex, int count, int msgId, int max) {
        if (endIndex.get() == count) {
            cleanOfflineCount(msgId);
            return;
        } else
            residurCall(endIndex, count, msgId, max);
    }

    /**
     * @Description 消费剩余消息
     * @author zhang bo
     * @date 18-10-26
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void consumResidueMsg(long position, AtomicInteger endIndex, int count, int msgId, int max) {
        logger.debug("consumResidueMsg params , position -> {} , endIndex -> {} , count -> {} , msgId -> {} , max -> {} , clientId -> {}"
                , position, endIndex, count, msgId, max, clientId);
        if (endIndex.get() == count) {
            this.closeFileHandle();
            return;
        }
        if (max > 0) {
            if (!isOfflineConsumState()) {
                this.closeFileHandle();
                return;
            }
            if (indexFile != null)
                indexFile.read(Buffer.buffer(CAPACITY), 0, position, CAPACITY, index -> {
                    if (index.failed()) {
                        logger.error(index.cause().getMessage(), index);
                        return;
                    } else {
                        int startOffset = index.result().getInt(2);
                        int endOffset = index.result().getInt(6);
                        int length = endOffset - startOffset;
                        byte valid = index.result().getByte(10);
                        long validtimestamp = index.result().getLong(20);
                        byte currentPartition = index.result().getByte(19);
                        if (valid == 0) {//有效
                            if (System.currentTimeMillis() - validtimestamp > liveTimeStamp) {//判断消息是否在有效时间内
                                endIndex.incrementAndGet();
                                processFlag(endIndex, count, msgId, max);
                                logger.debug("consumResidueMsg timeout, clientId -> {} , msgId -> {}", clientId, msgId);
                            } else {
                                AsyncFile tempAsyncFile;
                                if (this.logsFileHandle != null && (tempAsyncFile = this.logsFileHandle[currentPartition]) != null) {
                                    tempAsyncFile.read(Buffer.buffer(length), 0, startOffset, length, result -> {
                                        if (result.failed()) {
                                            logger.error(result.cause().getMessage(), result);
                                            closeFileHandle();
                                            return;
                                        } else {
                                            logger.debug("consumResidueMsg result , payload -> {} ,  clientId -> {} ", result.result().toString(), clientId);

                                            if (startOffset == 0 && endOffset == 0) {//本消息没有消费,跳过
                                                processFlag(endIndex, count, msgId, max);
                                            } else {
                                                if (vertx != null) {
                                                    vertx.setTimer(150 + endIndex.get(), timeId -> {
                                                        if (isOfflineConsumState() && result.result().length() > 0) {
                                                            if (vertx != null)
                                                                vertx.eventBus().send(MessageAddr.class.getName() + SEND_STORAGE_MSG,
                                                                        new JsonObject(result.result().toString()), new DeliveryOptions().addHeader("msgId"
                                                                                , String.valueOf(index.result().getShort(0) & 0x0FFFF))
                                                                                .addHeader("topic", getTopic())
                                                                                .addHeader("qos", String.valueOf(index.result().getByte(28)))
                                                                                .addHeader("uid", clientId));
                                                        }
                                                    });
                                                    endIndex.incrementAndGet();
                                                    processFlag(endIndex, count, msgId, max);
                                                } else {
                                                    return;
                                                }
                                            }
                                        }
                                    });
                                } else {
                                    logger.error("get file handle fail ,clienId -> {} , index -> {}", clientId, partition);
                                    return;
                                }
                            }
                        } else {
                            residurCall(endIndex, count, msgId, max);
                        }
                    }
                });
            else
                return;
        } else {
            if (endIndex.get() != count)
                cleanOfflineCount(msgId);
            return;
        }
    }


    /**
     * @Description 获取用户所有log句柄
     * @author zhang bo
     * @date 18-11-1
     * @version 1.0
     */
    private void getofflineLog() {
        File file = new File(CLIENT_DIR);
        FilenameFilter fnFilter = (File dir, String name) -> Pattern.matches(REGEX, name);
        String[] fileStrs = file.list(fnFilter);
        List<Integer> files = Arrays.stream(fileStrs).map(e -> {
            String[] arrs = e.split("-");
            if (arrs.length == 2) {
                return Integer.parseInt(arrs[1].substring(0, arrs[1].length() - 4));
            } else {
                return Integer.parseInt(arrs[1]);
            }
        }).collect(Collectors.toList());
        this.logsFileHandle = new AsyncFile[MAX_PARTITION];
        for (int i = 0; i < files.size(); i++) {
            this.logsFileHandle[files.get(i)] = fileSystem.openBlocking(CLIENT_DIR + "/" + fileStrs[i], new OpenOptions().setAppend(true).setRead(true).setWrite(true));
        }
    }


    @Override
    public void processOfflineMsg() {
        logger.debug("processOfflineLog , clientId -> {}", clientId);

        int stateSize = (int) new File(DIR_PATH + "/" + clientId + "/" + clientId + STATE_SUFFIX).length();

        if (stateSize >= 6) {
            if (vertx != null)
                vertx.executeBlocking(future -> {
                    if (this.stateFile != null)
                        stateFile.read(Buffer.buffer(4), 0, stateSize - 6, 4, res -> {
                            if (res.failed()) {
                                logger.error(res.cause().getMessage(), res);
                            } else {
                                int count = res.result().getShort(2) & 0x0FFFF;//未消費消息條數
                                logger.debug("processOfflineMsg ,count -> {} , clientId -> {} ", count, clientId);
                                if (count > 0) {//存在未消費消息
                                    getofflineLog();
                                    int msgId = res.result().getShort(0) & 0x0FFFF;
                                    AtomicInteger endIndex = new AtomicInteger(0);
                                    if (!isOfflineConsumState()) {
                                        this.closeFileHandle();
                                        return;
                                    }
                                    long position = getPrePoition(msgId);
                                    if (position <= 0) {
                                        cleanOfflineCount(msgId);
                                        return;
                                    }
                                    consumResidueMsg(position, endIndex, count, msgId, count);
                                }
                            }
                        });
                }, false, null);
        }
    }


    @Override
    public void updateTimerId(int msgId, long timerId, Handler<AsyncResult<Boolean>> handler) {
        if (indexFile != null)
            indexFile.write(Buffer.buffer(8).appendLong(timerId), getOffsetAddr(msgId) + 11, rs -> {
                if (rs.failed()) {
                    handler.handle(Future.failedFuture(rs.cause()));
                } else {
                    handler.handle(Future.succeededFuture(true));
                }
            });
    }
}
