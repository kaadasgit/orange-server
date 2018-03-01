package cn.orangeiot.sip.message;

import cn.orangeiot.sip.SipVertxFactory;
import cn.orangeiot.sip.constant.SipOptions;
import cn.orangeiot.sip.handler.PorcessHandler;
import io.vertx.core.datagram.DatagramSocketOptions;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.SocketAddress;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sip.SipFactory;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-02-02
 */
public class ResponseMsgUtil {

    private static Logger logger = LogManager.getLogger(ResponseMsgUtil.class);

    /**
     * @Description send Mesaage
     * @author zhang bo
     * @date 18-2-2
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public static void sendMessage(String username, String msg, SipOptions sipOptions) {
        Object netSocket = PorcessHandler.getNetSocketList().get(username);
        if (Objects.nonNull(netSocket)) {
            logger.info("==ResponseMsgUtil==sendMessage send  username->\n" + username);
            logger.info("==ResponseMsgUtil==sendMessage send  package->\n" + msg);
            if (sipOptions == SipOptions.UDP) {
                SocketAddress socket = (SocketAddress) netSocket;
                logger.info("==ResponseMsgUtil==sendMessage send  UDP host -> {},port -> {}->\n", socket.host(), socket.port());
                SipVertxFactory.getSocketInstance().send(msg, socket.port(), socket.host(), rs -> {
                    if (rs.failed()) {
                        rs.cause().printStackTrace();
                    } else {
                        if (rs.failed())
                            reSend(msg, socket);
                    }
                });
            } else {
                NetSocket socket = (NetSocket) netSocket;
                socket.write(msg);
                if (socket.writeQueueFull()) {
                    socket.pause();
                    socket.drainHandler(done -> socket.resume());
                }
            }
        }
    }


    /**
     * @param msg    消息
     * @param socket 客户端信息
     * @Description 重发消息
     * @author zhang bo
     * @date 18-2-27
     * @version 1.0
     */
    public static void reSend(String msg, SocketAddress socket) {
        logger.info("==ResponseMsgUtil==reSend==params -> msg = {} , socket = {}", msg, socket);
        //todo 重发消息
        AtomicInteger atomicInteger = new AtomicInteger(0);
        SipVertxFactory.getVertx().setPeriodic(SipVertxFactory.getConfig().getLong("intervalTimes"), rs -> {
            atomicInteger.getAndIncrement();//原子自增
            if (atomicInteger.intValue() == SipVertxFactory.getConfig().getInteger("maxTimes")) {//达到重发次数
                SipVertxFactory.getVertx().cancelTimer(rs);//取消周期定时
            }

            SipVertxFactory.getSocketInstance().send(msg, socket.port(), socket.host(), as -> {
                if (as.failed()) {
                    as.cause().printStackTrace();
                } else {
                    if (as.succeeded())
                        SipVertxFactory.getVertx().cancelTimer(rs);//取消周期定时
                }
            });
        });
    }

}
