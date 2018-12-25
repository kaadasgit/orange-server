package cn.orangeiot.sip.handler;

import cn.orangeiot.reg.user.UserAddr;
import cn.orangeiot.sip.constant.SipOptions;
import cn.orangeiot.sip.message.ResponseMsgUtil;
import gov.nist.javax.sip.address.SipUri;
import gov.nist.javax.sip.header.To;
import gov.nist.javax.sip.message.SIPRequest;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.SocketAddress;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sip.address.Address;
import javax.sip.address.SipURI;
import javax.sip.address.URI;
import javax.sip.header.ContactHeader;
import javax.sip.message.MessageFactory;
import javax.sip.message.Response;
import java.text.ParseException;
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

    private JsonObject conf;

    public RegisterHandler(MessageFactory msgFactory, JsonObject conf) {
        this.msgFactory = msgFactory;
        this.conf = conf;
    }

    /**
     * @Description 注冊處理
     * @author zhang bo
     * @date 18-2-2
     * @version 1.0
     */
    public void processRegister(SIPRequest request, SipOptions sipOptions, SocketAddress socketAddress, Vertx vertx) {
        Response response = null;
        To to = (To) request.getHeader(To.NAME);
        Address toAddress = to.getAddress();
        URI toURI = toAddress.getURI();

        SipURI uri = (SipURI) to.getAddress().getURI();
        String uid = uri.getUser();
        ContactHeader contactHeader = (ContactHeader) request.getHeader("Contact");
        Address contactAddr = contactHeader.getAddress();
        SipUri contactURI = (SipUri) contactAddr.getURI();
        logger.debug("processRegister from: " + toURI + " request str: " + contactURI);
        int expires = Objects.nonNull(request.getExpires()) ? request.getExpires().getExpires() : 0;
        // 如果expires不等于0,则为注册，否则为注销。
        boolean flag = true;//注冊
        if (expires != 0) {
//            if (Objects.nonNull(netSocket))
//                pool.put(toURI.toString(), netSocket);
//            else
//            vertx.eventBus().send(UserAddr.class.getName() + SAVE_REGISTER_USER,
//                    new JsonObject().put("uri", toURI.toString()).put("socketAddress", socketAddress.toString())
//                            .put("expires", expires));
            expires = expires > conf.getInteger("maxHeartIdleTime") ? conf.getInteger("maxHeartIdleTime") : expires;
            vertx.eventBus().send(UserAddr.class.getName() + SAVE_REGISTER_USER,
                    new JsonObject().put("uri", uid).put("socketAddress", socketAddress.toString())
                            .put("expires", expires));
            logger.debug("register user " + toURI);
        } else {
            flag = false;
        }

        try {
            if (!contactURI.getHostPort().equals(socketAddress.toString())) {
                contactURI.setHost(socketAddress.host());
                contactURI.setPort(socketAddress.port());
            }
            response = msgFactory.createResponse(200, request);//回复code200的成功
            response.setHeader(contactHeader);
            if (Objects.nonNull(request.getExpires()))
                response.setExpires(request.getExpires());
            if (flag)
                ResponseMsgUtil.sendMessage(toURI.toString(), response.toString(), sipOptions, uid);
            else
                ResponseMsgUtil.sendMessageAndClean(vertx, toURI.toString(), response.toString(), sipOptions, uid);
        } catch (ParseException e) {
            logger.error(e.getMessage(), e);
        }
    }
}
