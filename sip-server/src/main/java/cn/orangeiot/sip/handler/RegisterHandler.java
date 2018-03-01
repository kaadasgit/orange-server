package cn.orangeiot.sip.handler;

import cn.orangeiot.sip.SipVertxFactory;
import cn.orangeiot.sip.constant.SipOptions;
import cn.orangeiot.sip.message.ResponseMsgUtil;
import gov.nist.javax.sip.message.SIPRequest;
import io.vertx.core.Vertx;
import io.vertx.core.datagram.DatagramSocket;
import io.vertx.core.datagram.DatagramSocketOptions;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.SocketAddress;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sip.address.Address;
import javax.sip.address.URI;
import javax.sip.header.ContactHeader;
import javax.sip.header.ToHeader;
import javax.sip.message.MessageFactory;
import javax.sip.message.Response;
import java.text.ParseException;
import java.util.Map;
import java.util.Objects;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-01-31
 */
public class RegisterHandler {

    private Map<String, Object> pool;

    private static Logger logger = LogManager.getLogger(RegisterHandler.class);

    private MessageFactory msgFactory;

    public RegisterHandler(Map<String, Object> pool, MessageFactory msgFactory) {
        this.pool = pool;
        this.msgFactory = msgFactory;
    }

    /**
     * @Description 注冊處理
     * @author zhang bo
     * @date 18-2-2
     * @version 1.0
     */
    public void processRegister(SIPRequest request, NetSocket netSocket, SipOptions sipOptions, SocketAddress socketAddress) {
        Response response = null;
        ToHeader head = (ToHeader) request.getHeader(ToHeader.NAME);
        Address toAddress = head.getAddress();
        URI toURI = toAddress.getURI();
        ContactHeader contactHeader = (ContactHeader) request.getHeader("Contact");
        Address contactAddr = contactHeader.getAddress();
        URI contactURI = contactAddr.getURI();
        logger.info("processRegister from: " + toURI + " request str: " + contactURI);
        int expires = Objects.nonNull(request.getExpires()) ? request.getExpires().getExpires() : 0;
        // 如果expires不等于0,则为注册，否则为注销。
        if (expires != 0 || contactHeader.getExpires() != 0) {
            if (Objects.nonNull(netSocket))
                pool.put(toURI.toString(), netSocket);
            else
                pool.put(toURI.toString(), socketAddress);
            logger.info("register user " + toURI);
        } else {
            pool.remove(toURI.toString());
            logger.info("unregister user " + toURI);
        }

        try {
            response = msgFactory.createResponse(200, request);//回复code200的成功
            ResponseMsgUtil.sendMessage(toURI.toString(), response.toString(), sipOptions);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}
