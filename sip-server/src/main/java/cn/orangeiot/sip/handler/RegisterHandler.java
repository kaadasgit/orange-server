package cn.orangeiot.sip.handler;

import cn.orangeiot.reg.user.UserAddr;
import cn.orangeiot.sip.constant.SipOptions;
import cn.orangeiot.sip.message.ResponseMsgUtil;
import gov.nist.javax.sip.address.SipUri;
import gov.nist.javax.sip.message.SIPRequest;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.SocketAddress;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sip.address.Address;
import javax.sip.address.URI;
import javax.sip.header.ContactHeader;
import javax.sip.header.HeaderFactory;
import javax.sip.header.ToHeader;
import javax.sip.message.MessageFactory;
import javax.sip.message.Response;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Objects;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-01-31
 */
public class RegisterHandler implements UserAddr {

    private static Logger logger = LogManager.getLogger(RegisterHandler.class);

    private MessageFactory msgFactory;

    private HeaderFactory headerFactory;

    public RegisterHandler(MessageFactory msgFactory, HeaderFactory headerFactory) {
        this.msgFactory = msgFactory;
        this.headerFactory = headerFactory;
    }

    /**
     * @Description 注冊處理
     * @author zhang bo
     * @date 18-2-2
     * @version 1.0
     */
    public void processRegister(SIPRequest request, NetSocket netSocket, SipOptions sipOptions, SocketAddress socketAddress, Vertx vertx) {
        Response response = null;
        ToHeader head = (ToHeader) request.getHeader(ToHeader.NAME);
        Address toAddress = head.getAddress();
        URI toURI = toAddress.getURI();
        ContactHeader contactHeader = (ContactHeader) request.getHeader("Contact");
        Address contactAddr = contactHeader.getAddress();
        SipUri contactURI = (SipUri) contactAddr.getURI();
        logger.info("processRegister from: " + toURI + " request str: " + contactURI);
        int expires = Objects.nonNull(request.getExpires()) ? request.getExpires().getExpires() : 0;
        // 如果expires不等于0,则为注册，否则为注销。
        if (expires != 0 || contactHeader.getExpires() != 0) {
//            if (Objects.nonNull(netSocket))
//                pool.put(toURI.toString(), netSocket);
//            else
            vertx.eventBus().send(UserAddr.class.getName() + SAVE_REGISTER_USER,
                    new JsonObject().put("uri", toURI.toString()).put("socketAddress", socketAddress.toString())
                            .put("Expires", contactHeader.getExpires()));
            logger.info("register user " + toURI);
        } else {
            vertx.eventBus().send(UserAddr.class.getName() + DEL_REGISTER_USER,
                    new JsonObject().put("uri", toURI.toString()));
            logger.info("unregister user " + toURI);
        }

        try {
            if (!contactURI.getHostPort().equals(socketAddress.toString())) {
                contactURI.setHost(socketAddress.host());
                contactURI.setPort(socketAddress.port());
            }
            response = msgFactory.createResponse(200, request);//回复code200的成功
            response.setHeader(contactHeader);
            response.setExpires(request.getExpires());
            ResponseMsgUtil.sendMessage(toURI.toString(), response.toString(), sipOptions);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}
