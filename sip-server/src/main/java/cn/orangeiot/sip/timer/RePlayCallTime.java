package cn.orangeiot.sip.timer;

import cn.orangeiot.common.options.SendOptions;
import cn.orangeiot.reg.user.UserAddr;
import cn.orangeiot.sip.SipVertxFactory;
import cn.orangeiot.sip.handler.PorcessHandler;
import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.Message;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.net.impl.SocketAddressImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-03-02
 */
public class RePlayCallTime implements UserAddr {

    private static Logger logger = LogManager.getLogger(RePlayCallTime.class);

    /**
     * @Description 周期重播
     * @author zhang bo
     * @date 18-3-2
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public static void callPeriodic(String msg, String username) {
        logger.debug("==ResponseMsgUtil==reSend==params -> msg = {} , socket = {}", msg, username);
        // 重发消息
        AtomicInteger atomicInteger = new AtomicInteger(0);
        SipVertxFactory.getVertx().setPeriodic(SipVertxFactory.getConfig().getLong("intervalTimes"), rs -> {
            SipVertxFactory.getVertx().eventBus().send(UserAddr.class.getName() + GET_REGISTER_USER,
                    username, SendOptions.getInstance(), (AsyncResult<Message<String>> as) -> {
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
                                });
                            }
                        }
                    });
        });
    }
}
