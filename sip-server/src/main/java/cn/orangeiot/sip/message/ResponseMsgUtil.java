package cn.orangeiot.sip.message;

import cn.orangeiot.common.options.SendOptions;
import cn.orangeiot.reg.user.UserAddr;
import cn.orangeiot.sip.SipVertxFactory;
import cn.orangeiot.sip.constant.SipOptions;
import cn.orangeiot.sip.handler.PorcessHandler;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.datagram.DatagramSocketOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.net.impl.SocketAddressImpl;
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
public class ResponseMsgUtil implements UserAddr {

    private static Logger logger = LogManager.getLogger(ResponseMsgUtil.class);

    /**
     * @Description send Mesaage
     * @author zhang bo
     * @date 18-2-2
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public static void sendMessage(String username, String msg, SipOptions sipOptions, String uid) {
        SipVertxFactory.getVertx().eventBus().send(UserAddr.class.getName() + GET_REGISTER_USER,
                uid, SendOptions.getInstance(), (AsyncResult<Message<String>> as) -> {
                    if (as.failed()) {
                        logger.error(as.cause().getMessage(), as.cause());
                    } else {
                        if (Objects.nonNull(as.result().body())) {
                            logger.debug("==ResponseMsgUtil==sendMessage send  username->\n" + username);
                            logger.debug("==ResponseMsgUtil==sendMessage send  package->\n" + msg);
                            if (sipOptions == SipOptions.UDP) {
                                String[] address = as.result().body().split(":");
                                SocketAddress socket = new SocketAddressImpl(Integer.parseInt(address[1]), address[0]);
                                SipVertxFactory.getSocketInstance().send(msg, socket.port(), socket.host(), rs -> {
                                    if (rs.failed()) {
                                        logger.error(rs.cause().getMessage(), rs.cause());
                                    } else {
                                        logger.debug("send success ->" + rs.succeeded());
                                        if (rs.failed())
                                            reSend(msg, username, uid);
                                    }
                                });
                            } else {
//                                NetSocket socket = (NetSocket) as.result().body();
//                                socket.write(msg);
//                                if (socket.writeQueueFull()) {
//                                    socket.pause();
//                                    socket.drainHandler(done -> socket.resume());
//                                }
                            }
                        }
                    }
                });

    }


    /**
     * @Description 回复消息, 并清除注册信息
     * @author zhang bo
     * @date 18-7-5
     * @version 1.0
     */
    public static void sendMessageAndClean(Vertx vertx, String username, String msg, SipOptions sipOptions, String uid) {
        SipVertxFactory.getVertx().eventBus().send(UserAddr.class.getName() + GET_REGISTER_USER,
                uid, SendOptions.getInstance(), (AsyncResult<Message<String>> as) -> {
                    if (as.failed()) {
                        logger.error(as.cause().getMessage(), as.cause());
                    } else {
                        if (Objects.nonNull(as.result().body())) {
                            logger.debug("==ResponseMsgUtil==sendMessage send  username->\n" + username);
                            logger.debug("==ResponseMsgUtil==sendMessage send  package->\n" + msg);
                            if (sipOptions == SipOptions.UDP) {
                                String[] address = as.result().body().split(":");
                                SocketAddress socket = new SocketAddressImpl(Integer.parseInt(address[1]), address[0]);
                                logger.info("==ResponseMsgUtil==sendMessage send  UDP host -> {},port -> {}->\n", socket.host(), socket.port());
                                SipVertxFactory.getSocketInstance().send(msg, socket.port(), socket.host(), rs -> {
                                    if (rs.failed()) {
                                        logger.error(rs.cause().getMessage(), rs.cause());
                                    } else {
                                        logger.debug("send success ->" + rs.succeeded());
                                        if (rs.failed())
                                            reSendAndClean(vertx, msg, username, uid);
                                        else {
                                            vertx.eventBus().send(UserAddr.class.getName() + DEL_REGISTER_USER,
                                                    new JsonObject().put("uri", uid));
                                            logger.warn("unregister user " + username);
                                        }
                                    }
                                });
                            } else {
//                                NetSocket socket = (NetSocket) as.result().body();
//                                socket.write(msg);
//                                if (socket.writeQueueFull()) {
//                                    socket.pause();
//                                    socket.drainHandler(done -> socket.resume());
//                                }
                            }
                        }
                    }
                });

    }


    /**
     * @param msg 消息
     * @Description 重发消息並清除用戶信息
     * @author zhang bo
     * @date 18-2-27
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public static void reSendAndClean(Vertx vertx, String msg, String username, String uid) {
        // 重发消息
        AtomicInteger atomicInteger = new AtomicInteger(0);
        SipVertxFactory.getVertx().setPeriodic(SipVertxFactory.getConfig().getLong("intervalTimes"), rs -> {
            SipVertxFactory.getVertx().eventBus().send(UserAddr.class.getName() + GET_REGISTER_USER,
                    uid, SendOptions.getInstance(), (AsyncResult<Message<String>> as) -> {
                        if (as.failed()) {
                            logger.error(as.cause().getMessage(), as.cause());
                        } else {
                            atomicInteger.getAndIncrement();//原子自增
                            if (atomicInteger.intValue() == SipVertxFactory.getConfig().getInteger("maxTimes")) {//达到重发次数
                                SipVertxFactory.getVertx().cancelTimer(rs);//取消周期定时
                            }

                            if (Objects.nonNull(as.result().body())) {
                                String[] address = as.result().body().split(":");
                                SocketAddress socket = new SocketAddressImpl(Integer.parseInt(address[1]), address[0]);
                                SipVertxFactory.getSocketInstance().send(msg, socket.port(), socket.host(), ars -> {
                                    if (ars.failed())
                                        logger.error(ars.cause().getMessage(), ars.cause());
                                    else {
                                        SipVertxFactory.getVertx().cancelTimer(rs);//取消周期定时
                                        vertx.eventBus().send(UserAddr.class.getName() + DEL_REGISTER_USER,
                                                new JsonObject().put("uri", username));
                                        logger.debug("unregister user " + username);
                                    }
                                });
                            }
                        }
                    });
        });
    }

    /**
     * @param msg 消息
     * @Description 重发消息
     * @author zhang bo
     * @date 18-2-27
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public static void reSend(String msg, String username, String uid) {
        logger.debug("==ResponseMsgUtil==reSend==params -> msg = {} , socket = {}", msg, username);
        // 重发消息
        AtomicInteger atomicInteger = new AtomicInteger(0);
        SipVertxFactory.getVertx().setPeriodic(SipVertxFactory.getConfig().getLong("intervalTimes"), rs -> {
            SipVertxFactory.getVertx().eventBus().send(UserAddr.class.getName() + GET_REGISTER_USER,
                    uid, SendOptions.getInstance(), (AsyncResult<Message<String>> as) -> {
                        if (as.failed()) {
                            logger.error(as.cause().getMessage(), as.cause());
                        } else {
                            atomicInteger.getAndIncrement();//原子自增
                            if (atomicInteger.intValue() == SipVertxFactory.getConfig().getInteger("maxTimes")) {//达到重发次数
                                SipVertxFactory.getVertx().cancelTimer(rs);//取消周期定时
                            }

                            if (Objects.nonNull(as.result().body())) {
                                String[] address = as.result().body().split(":");
                                SocketAddress socket = new SocketAddressImpl(Integer.parseInt(address[1]), address[0]);
                                SipVertxFactory.getSocketInstance().send(msg, socket.port(), socket.host(), ars -> {
                                    if (ars.failed())
                                        logger.error(ars.cause().getMessage(), ars.cause());
                                    else
                                        SipVertxFactory.getVertx().cancelTimer(rs);//取消周期定时
                                });
                            }
                        }
                    });
        });
    }

}
