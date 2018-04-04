package cn.orangeiot.rtp.handler;

import cn.orangeiot.common.constant.MediaTypeEnum;
import cn.orangeiot.common.options.SendOptions;
import cn.orangeiot.reg.user.UserAddr;
import cn.orangeiot.rtp.RtpVertFactory;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.datagram.DatagramPacket;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-03-08
 */
public class ProcessHandler implements UserAddr {

    private static Logger logger = LogManager.getLogger(ProcessHandler.class);

    private JsonObject conf;

    private Vertx vertx;

    public ProcessHandler(JsonObject conf, Vertx vertx) {
        this.vertx = vertx;
        this.conf = conf;
    }


    /**
     * @Description rtp stream video 处理
     * @author zhang bo
     * @date 18-3-8
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void videoProcess(DatagramPacket datagramPacket) {
        logger.info("SERVER received remoteAddress: " + datagramPacket.sender().toString());
        logger.info("SERVER I received some bytes: " + datagramPacket.data().length());


        //TODO 轉發rtp stream
        vertx.eventBus().send(UserAddr.class.getName() + GET_CALL_ID, MediaTypeEnum.VIDEO.toString().toLowerCase() + datagramPacket.sender().host()
                , SendOptions.getInstance(), (AsyncResult<Message<String>> rs) -> {
                    if (rs.failed()) {
                        rs.cause().printStackTrace();
                    } else {
                        if (Objects.nonNull(rs.result().body())) {
                            String[] socketAddress = rs.result().body().split(":");
                            RtpVertFactory.getVideoSocket().send(datagramPacket.data(), Integer.parseInt(socketAddress[1]), socketAddress[0]
                                    , as -> {
                                        if (as.failed()) {
                                            as.cause().printStackTrace();
                                        } else {
                                            logger.info("send success ->" + rs.succeeded());
                                            if (as.failed())
                                                reSendVideo(datagramPacket.data(), socketAddress);
                                        }
                                    });
                        }
                    }
                });
    }


    /**
     * @Description video重发消息
     * @author zhang bo
     * @date 18-2-27
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void reSendVideo(Buffer bufferStream, String[] socketAddress) {
        logger.info("==ResponseMsgUtil==reSendVideo==params -> socket = {}", socketAddress);
        //todo 重发消息
        AtomicInteger atomicInteger = new AtomicInteger(0);
        vertx.setPeriodic(conf.getLong("intervalTimes"), rs -> {
            atomicInteger.getAndIncrement();//原子自增
            if (atomicInteger.intValue() == conf.getInteger("maxTimes")) {//达到重发次数
                vertx.cancelTimer(rs);//取消周期定时
            }
            RtpVertFactory.getVideoSocket().send(bufferStream, Integer.parseInt(socketAddress[1]), socketAddress[0], ars -> {
                if (ars.failed())
                    ars.cause().printStackTrace();
                else
                    vertx.cancelTimer(rs);//取消周期定时
            });
        });
    }


    /**
     * @Description rtp stream audio 处理
     * @author zhang bo
     * @date 18-3-8
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void audioProcess(DatagramPacket datagramPacket) {
        logger.info("SERVER received remoteAddress: " + datagramPacket.sender().toString());
        logger.info("SERVER I received some bytes: " + datagramPacket.data().length());

        //TODO 轉發rtp stream
        vertx.eventBus().send(UserAddr.class.getName() + GET_CALL_ID, MediaTypeEnum.AUDIO.toString().toLowerCase() + datagramPacket.sender().host()
                , SendOptions.getInstance(), (AsyncResult<Message<String>> rs) -> {
                    if (rs.failed()) {
                        rs.cause().printStackTrace();
                    } else {
                        if (Objects.nonNull(rs.result().body())) {
                            String[] socketAddress = rs.result().body().split(":");
                            RtpVertFactory.getAudioSocket().send(datagramPacket.data(), Integer.parseInt(socketAddress[1]), socketAddress[0]
                                    , as -> {
                                        if (as.failed()) {
                                            as.cause().printStackTrace();
                                        } else {
                                            logger.info("send success ->" + rs.succeeded());
                                            if (as.failed())
                                                reSendAudio(datagramPacket.data(), socketAddress);
                                        }
                                    });
                        }
                    }
                });
    }


    /**
     * @Description audio重发消息
     * @author zhang bo
     * @date 18-2-27
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void reSendAudio(Buffer bufferStream, String[] socketAddress) {
        logger.info("==ResponseMsgUtil==reSendAudio==params -> socket = {}", socketAddress);
        //todo 重发消息
        AtomicInteger atomicInteger = new AtomicInteger(0);
        vertx.setPeriodic(conf.getLong("intervalTimes"), rs -> {
            atomicInteger.getAndIncrement();//原子自增
            if (atomicInteger.intValue() == conf.getInteger("maxTimes")) {//达到重发次数
                vertx.cancelTimer(rs);//取消周期定时
            }
            RtpVertFactory.getVideoSocket().send(bufferStream, Integer.parseInt(socketAddress[1]), socketAddress[0], ars -> {
                if (ars.failed())
                    ars.cause().printStackTrace();
                else
                    vertx.cancelTimer(rs);//取消周期定时
            });
        });
    }

}
