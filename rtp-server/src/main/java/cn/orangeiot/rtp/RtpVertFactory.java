package cn.orangeiot.rtp;

import cn.orangeiot.rtp.handler.ProcessHandler;
import io.vertx.core.Vertx;
import io.vertx.core.datagram.DatagramSocket;
import io.vertx.core.datagram.DatagramSocketOptions;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Objects;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-03-08
 */
public class RtpVertFactory {

    private static RtpVertFactory myFactory = null;

    private static Logger logger = LogManager.getLogger(RtpVertFactory.class);

    private static Vertx vertx;

    private static JsonObject config;

    private static DatagramSocket videoSocket = null;

    private static DatagramSocket audioSocket = null;

    public static synchronized RtpVertFactory getInstance() {
        if (myFactory == null) {
            myFactory = new RtpVertFactory();
        }
        return myFactory;
    }


    public static DatagramSocket getVideoSocket() {
        return videoSocket;
    }


    public static DatagramSocket getAudioSocket() {
        return audioSocket;
    }

    /**
     * @Description 启动监听
     * @author zhang bo
     * @date 18-1-30
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void createListeningPoint(Vertx vertx, JsonObject jsonObject) {
        Objects.requireNonNull(vertx);
        Objects.requireNonNull(jsonObject);
        this.vertx = vertx;
        this.config = jsonObject;

        videoSocket = vertx.createDatagramSocket(new DatagramSocketOptions()
                .setReuseAddress(true));

        ProcessHandler porcessHandler = new ProcessHandler(jsonObject, vertx);

        videoSocket.listen(jsonObject.getInteger("videoPort"), jsonObject.getString("videoHost"), asyncResult -> {
            if (asyncResult.succeeded()) {
                videoSocket.handler(porcessHandler::videoProcess);//数据包处理
            } else {
                logger.error("Listen failed" + asyncResult.cause());
                logger.error("Failed to bind!");
                System.exit(0);
            }
        });

        audioSocket = vertx.createDatagramSocket(new DatagramSocketOptions()
                .setReuseAddress(true));

        audioSocket.listen(jsonObject.getInteger("audioPort"), jsonObject.getString("audioHost"), asyncResult -> {
            if (asyncResult.succeeded()) {
                audioSocket.handler(porcessHandler::audioProcess);//数据包处理
            } else {
                logger.error("Listen failed" + asyncResult.cause());
                logger.error("Failed to bind!");
                System.exit(0);
            }
        });

    }


}
