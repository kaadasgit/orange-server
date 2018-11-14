package cn.orangeiot.sip.handler;

import cn.orangeiot.common.constant.MediaTypeEnum;
import cn.orangeiot.common.options.SendOptions;
import cn.orangeiot.reg.user.UserAddr;
import cn.orangeiot.sip.SipVertxFactory;
import cn.orangeiot.sip.constant.SipOptions;
import cn.orangeiot.sip.message.ResponseMsgUtil;
import gov.nist.javax.sdp.SessionDescriptionImpl;
import gov.nist.javax.sdp.parser.SDPAnnounceParser;
import gov.nist.javax.sip.header.*;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.SocketAddress;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sdp.MediaDescription;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.header.ContactHeader;
import javax.sip.header.HeaderFactory;
import javax.sip.header.ToHeader;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.text.ParseException;
import java.util.Objects;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-02-02
 */
public class ResponseHandler implements UserAddr {

    private static Logger logger = LogManager.getLogger(ResponseHandler.class);

    private MessageFactory msgFactory;

    private HeaderFactory headerFactory;

    private AddressFactory addressFactory;

    private JsonObject jsonObject;


    public ResponseHandler(MessageFactory msgFactory, HeaderFactory headerFactory, AddressFactory addressFactory
            , JsonObject jsonObject) {
        this.msgFactory = msgFactory;
        this.headerFactory = headerFactory;
        this.addressFactory = addressFactory;
        this.jsonObject = jsonObject;
    }


    /**
     * @param response   客户端回复响应对象
     * @param sipOptions sip属性
     * @param uid        用户id
     * @param code       状态码
     * @Description 处理消息并发送
     * @author zhang bo
     * @date 18-11-14
     * @version 1.0
     */
    public void processMsgAndSendClient(SIPResponse response, SipOptions sipOptions, String uid, int code) {
        Response callerResp = null;
        try {
            callerResp = msgFactory.createResponse(code, response.getCallId(),
                    response.getCSeq(), response.getFrom(), response.getTo()
                    , response.getViaHeaders(), new MaxForwards(70));

            // 更新contact头域值，因为后面的消息是根据该URI来路由的
            ContactHeader contactHeader = headerFactory.createContactHeader();
            Address address = addressFactory.createAddress("sip:sipServer@" + jsonObject.getString("host") + ":" + jsonObject.getInteger("port"));
            contactHeader.setAddress(address);
            contactHeader.setExpires(3600);
            callerResp.addHeader(contactHeader);

            if (Objects.nonNull(response.getReasonPhrase(code)))
                callerResp.setReasonPhrase(response.getReasonPhrase(code));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        // 拷贝to头域
        ToHeader toHeader = (ToHeader) response.getHeader(ToHeader.NAME);
        callerResp.setHeader(toHeader);

        // 拷贝相应的消息体
        ContentLength contentLen = (ContentLength) response.getContentLength();
        if (contentLen != null && contentLen.getContentLength() != 0) {
            ContentType contentType = (ContentType) response.getHeader(ContentType.NAME);
            logger.debug("the sdp contenttype is " + contentType);

            callerResp.setContentLength(contentLen);
            try {
                callerResp.setContent(response.getContent(), contentType);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        } else {
            logger.warn("sdp is null.");
        }

        if (Objects.nonNull(uid)) {
            ResponseMsgUtil.sendMessage(uid, callerResp.toString(), sipOptions, uid);
        } else {
            logger.warn("uid is null.");
        }
    }

    /**
     * @Description 不存在callId, 會話過期
     * @author zhang bo
     * @date 18-11-14
     * @version 1.0
     */
    public void noExistsCall(SIPResponse sipResponse, SocketAddress socketAddress) {
        SIPRequest sipRequest = new SIPRequest();
        sipRequest.setCallId(sipResponse.getCallId());
        sipRequest.setCSeq(sipResponse.getCSeqHeader());
        sipRequest.setVia(sipResponse.getViaHeaders());
        sipRequest.setFrom(sipResponse.getFromHeader());
        sipRequest.setTo(sipResponse.getToHeader());
        try {
            Response newResponse = msgFactory.createResponse(Response.CALL_OR_TRANSACTION_DOES_NOT_EXIST, sipRequest);
            SipVertxFactory.getSocketInstance().send(newResponse.toString(), socketAddress.port(), socketAddress.host(), rs -> {
                if (rs.failed()) {
                    logger.error(rs.cause().getMessage(), rs.cause());
                } else {
                    logger.debug("send success ->" + rs.succeeded());
                }
            });
        } catch (ParseException e) {
            logger.error(e);
        }
    }


    /**
     * @Description 回包處理
     * @author zhang bo
     * @date 18-2-2
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void processResponse(SIPResponse response, SipOptions sipOptions, SocketAddress socketAddress, Vertx vertx) {
        int code = response.getStatusCode();
//        logger.debug("==PorcessHandler==processResponse===request content====" + response.getContent());
        logger.debug("==PorcessHandler==processResponse===request method====" + code);

        if (code == Response.TRYING) {
            logger.debug("The response is 100 response.");
            return;
        } else if (code == Response.OK && response.getCSeq().getMethod() == Request.BYE) {
            From from = (From) response.getHeader(From.NAME);
            SipURI resUri = (SipURI) from.getAddress().getURI();
            String uid = resUri.getUser();
            processMsgAndSendClient(response, sipOptions, uid, code);
        } else {
            CallID callID = (CallID) response.getHeader(CallID.NAME);

            vertx.eventBus().send(UserAddr.class.getName() + GET_SESSION_BRANCH, callID.getCallIdentifer().toString(), SendOptions.getInstance(), (AsyncResult<Message<String>> res) -> {
                if (res.failed()) {
                    logger.error(res.cause());
                    noExistsCall(response, socketAddress);
                } else {
                    if (res.result() != null && res.result().body() != null) {
                        processMsgAndSendClient(response, sipOptions, res.result().body(), code);

                        if (code == Response.DECLINE)//丢弃会话
                            vertx.eventBus().send(UserAddr.class.getName() + REMOVE_SESSION_BRANCH, callID.getCallIdentifer().toString());
                    } else {
                        noExistsCall(response, socketAddress);
                    }
                }
            });

        }


    }
}
