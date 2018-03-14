package cn.orangeiot.sip.handler;

import cn.orangeiot.common.constant.MediaTypeEnum;
import cn.orangeiot.reg.user.UserAddr;
import cn.orangeiot.sip.SipVertxFactory;
import cn.orangeiot.sip.constant.SipOptions;
import cn.orangeiot.sip.message.ResponseMsgUtil;
import gov.nist.javax.sdp.SessionDescriptionImpl;
import gov.nist.javax.sdp.parser.SDPAnnounceParser;
import gov.nist.javax.sip.header.*;
import gov.nist.javax.sip.message.SIPResponse;
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
     * @Description 回包處理
     * @author zhang bo
     * @date 18-2-2
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void processResponse(SIPResponse response, SipOptions sipOptions) {
        int code = response.getStatusCode();
        logger.info("==PorcessHandler==processResponse===request content====" + response.getContent());
        logger.info("==PorcessHandler==processResponse===request method====" + code);

        Response callerResp = null;
        String uri = "";
        Via via = null;
        if (code == Response.TRYING) {
            logger.info("The response is 100 response.");
            return;
        } else if (code == Response.OK && response.getCSeq().getMethod() == Request.BYE) {
            From from = (From) response.getHeader(From.NAME);
            uri = from.getAddress().getURI().toString();
        } else {
            via = (Via) response.getHeader(Via.NAME);
            uri = PorcessHandler.getBranchs().get(via.getBranch());
        }

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
            e.printStackTrace();
        }

        // 拷贝to头域
        ToHeader toHeader = (ToHeader) response.getHeader(ToHeader.NAME);
        callerResp.setHeader(toHeader);
        CallID callID = (CallID) response.getHeader(CallID.NAME);


        // 拷贝相应的消息体
        ContentLength contentLen = (ContentLength) response.getContentLength();
        if (contentLen != null && contentLen.getContentLength() != 0) {
            ContentType contentType = (ContentType) response.getHeader(ContentType.NAME);
            logger.info("the sdp contenttype is " + contentType);

            callerResp.setContentLength(contentLen);
            try {
                if (response.getCSeq().getMethod() == Request.INVITE) {//relay
                    //todo sdp解析
                    String contents = response.getMultipartMimeContent().getContents().next().getContent().toString();
                    SDPAnnounceParser parser = new SDPAnnounceParser(contents);
                    SessionDescriptionImpl parsedDescription = parser.parse();
                    //回复100 Trying
                    ContactHeader contactHeader = (ContactHeader) response.getHeader("Contact");
                    SipURI sipURI = (SipURI) contactHeader.getAddress().getURI();
//                    if (!parsedDescription.getConnection().getAddress().equals(sipURI.getHost())) {//relay
                    String finalUri = uri;
                    final String ipAddress = parsedDescription.getConnection().getAddress();
                    parsedDescription.getMediaDescriptions(false)
                            .forEach(e -> {
                                MediaDescription md = (MediaDescription) e;
                                Object cc = md.getAttributes(false).stream().filter(r -> r.toString().startsWith("a=candidate"))
                                        .filter(r -> r.toString().startsWith("a=candidate:2")).findFirst().orElseGet(null);
                                String host = "";
                                String port = "";
                                if (Objects.nonNull(cc)) {
                                    String[] address = cc.toString().split("\\s+");
                                    host = address[4];
                                    port = address[5];
                                    System.out.println("host:" + host + "::port:" + port);
                                }
                                try {
                                    if (md.getMedia().getMediaType().equalsIgnoreCase(MediaTypeEnum.AUDIO.toString())) {
                                        //保存映射關系
                                        SipVertxFactory.getVertx().eventBus().send(UserAddr.class.getName() + SAVE_CALL_ID
                                                , new JsonObject().put("receAddr", host + ":" + port)
                                                        .put("sendAddr", finalUri)
                                                        .put("mediaType", md.getMedia().getMediaType().toLowerCase()));
                                    } else {
                                        //保存映射關系
                                        SipVertxFactory.getVertx().eventBus().send(UserAddr.class.getName() + SAVE_CALL_ID
                                                , new JsonObject().put("receAddr", host + ":" + port)
                                                        .put("sendAddr", finalUri)
                                                        .put("mediaType", md.getMedia().getMediaType().toLowerCase()));
                                    }
                                    parsedDescription.getOrigin().setAddress(jsonObject.getString(md.getMedia().getMediaType().toLowerCase() + "Host"));//修改本地地址
                                    parsedDescription.getConnection().setAddress(jsonObject.getString(md.getMedia().getMediaType().toLowerCase() + "Host"));//修改接收地址
                                    md.getMedia().setMediaPort(jsonObject.getInteger(md.getMedia().getMediaType().toLowerCase() + "Port"));//修改接收端口
                                } catch (Exception e1) {
                                    e1.printStackTrace();
                                }
                            });

                    callerResp.setContentLength(new ContentLength(parsedDescription.toString().length()));
                    callerResp.setContent(parsedDescription.toString(), contentType);
                } else {
                    callerResp.setContent(response.getContent(), contentType);
                }
//                } else {
//                    callerResp.setContent(response.getContent(), contentType);
//                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            logger.info("sdp is null.");
        }

        if (Objects.nonNull(uri)) {
            ResponseMsgUtil.sendMessage(uri, callerResp.toString(), sipOptions);
        } else {
            logger.error("uri is null.");
        }

        logger.info("send response to caller : " + callerResp.toString());
    }
}
