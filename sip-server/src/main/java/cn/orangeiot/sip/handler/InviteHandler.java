package cn.orangeiot.sip.handler;

import cn.orangeiot.reg.user.UserAddr;
import cn.orangeiot.sip.constant.SipOptions;
import cn.orangeiot.sip.message.ResponseMsgUtil;
import gov.nist.javax.sip.header.CallID;
import gov.nist.javax.sip.header.To;
import gov.nist.javax.sip.header.Via;
import gov.nist.javax.sip.header.ViaList;
import gov.nist.javax.sip.message.SIPRequest;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.SocketAddress;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.address.URI;
import javax.sip.header.ContactHeader;
import javax.sip.header.HeaderFactory;
import javax.sip.message.MessageFactory;
import javax.sip.message.Response;
import java.text.ParseException;
import java.util.Objects;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-02-02
 */
public class InviteHandler implements UserAddr {

    private static Logger logger = LogManager.getLogger(InviteHandler.class);

    private MessageFactory msgFactory;

    private HeaderFactory headerFactory;

    private JsonObject jsonObject;

    private AddressFactory addressFactory;

    public InviteHandler(MessageFactory msgFactory
            , HeaderFactory headerFactory, JsonObject jsonObject, AddressFactory addressFactory) {
        this.msgFactory = msgFactory;
        this.headerFactory = headerFactory;
        this.jsonObject = jsonObject;
        this.addressFactory = addressFactory;
    }


    /**
     * @Description invite請求處理
     * @author zhang bo
     * @date 18-2-2
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void processInvite(SIPRequest request, SipOptions sipOptions,Vertx vertx) {
        //查询是否存在相关账户信息
        To to = (To) request.getHeader(To.NAME);
        Via via = (Via) request.getHeader(Via.NAME);

        vertx.eventBus().send(UserAddr.class.getName() + GET_REGISTER_USER, to.getAddress().getURI().toString(), rs -> {
            if (rs.failed()) {
                logger.error(rs.cause().getMessage(), rs);
                notExistsUser(request, sipOptions);
            } else {
                if (Objects.nonNull(rs.result().body())) {
                    try {
                        //回复100 Trying
                        ContactHeader contactHeader = (ContactHeader) request.getHeader("Contact");
                        Address contactAddr = contactHeader.getAddress();
                        SipURI sipURI = (SipURI) contactHeader.getAddress().getURI();

                        Response response = msgFactory.createResponse(Response.TRYING, request);
                        ResponseMsgUtil.sendMessage(request.getFrom().getAddress().getURI().toString()
                                , response.toString(), sipOptions);


                        CallID callID = (CallID) request.getHeader(CallID.NAME);
                        // sdp解析
//            String contents = request.getMultipartMimeContent().getContents().next().getContent().toString();
//            SDPAnnounceParser parser = new SDPAnnounceParser(contents);
//            SessionDescriptionImpl parsedDescription = parser.parse();
//            if (!parsedDescription.getConnection().getAddress().equals(sipURI.getHost())) {//relay
//            final String ipAddress = parsedDescription.getConnection().getAddress();
//            parsedDescription.getMediaDescriptions(false)
//                    .forEach(e -> {
//                        MediaDescription md = (MediaDescription) e;
//                        try {
//                            SipVertxFactory.getVertx().eventBus().send(UserAddr.class.getName() + SAVE_CALL_ID
//                                    , new JsonObject().put("receAddr", ipAddress + ":" + md.getMedia().getMediaPort())
//                                            .put("sendAddr", to.getAddress().getURI().toString())
//                                            .put("mediaType", md.getMedia().getMediaType().toLowerCase()));
//                            parsedDescription.getOrigin().setAddress(jsonObject.getString(md.getMedia().getMediaType().toLowerCase() + "Host"));//修改本地地址
//                            parsedDescription.getConnection().setAddress(jsonObject.getString(md.getMedia().getMediaType().toLowerCase() + "Host"));//修改接收地址
//                            md.getMedia().setMediaPort(jsonObject.getInteger(md.getMedia().getMediaType().toLowerCase() + "Port"));//修改接收端口
//                        } catch (Exception e1) {
//                            e1.printStackTrace();
//                        }
//                    });
//            request.setContentLength(new ContentLength(parsedDescription.toString().length()));
//            ContentType contentType = (ContentType) request.getHeader(ContentType.NAME);
//            request.setContent(parsedDescription.toString(), contentType);
//        }

                        //根据Request uri来路由，后续的响应消息通过VIA来路由
                        URI contactURI = contactAddr.getURI();
                        logger.info("processInvite contact=" + contactURI);
                        request.setRequestURI(contactURI);

//            Via callerVia = (Via) request.getHeader(Via.NAME);
//            Via via = (Via) headerFactory.createViaHeader(jsonObject.getString("host"), jsonObject.getInteger("port")
//                    , sipOptions.toString(), callerVia.getBranch());

                        // 更新contact的地址
                        ContactHeader contactHeaders = headerFactory.createContactHeader();
                        Address address = addressFactory.createAddress("sip:sipServer@" + jsonObject.getString("host") + ":" + jsonObject.getInteger("port"));
                        contactHeaders.setAddress(address);
                        contactHeaders.setExpires(3600);
                        request.setHeader(contactHeaders);


                        // 需要测试是否能够通过设置VIA头域来修改VIA头域值
                        if (request.getViaHeaders().size() > 1) {
                            ViaList viaList = request.getViaHeaders();
                            // 需要测试是否能够通过设置VIA头域来修改VIA头域值
//                request.removeHeader(Via.NAME);
                            request.getViaHeaders().forEach(e -> {
                                if (Objects.nonNull(e.getRPort())) {
                                    try {
                                        e.setReceived(jsonObject.getString("host"));
                                        e.setParameter(Via.RPORT, String.valueOf(jsonObject.getInteger("port")));
                                    } catch (ParseException e1) {
                                        e1.printStackTrace();
                                    }
                                }
                            });
                        } else {
                            // 需要测试是否能够通过设置VIA头域来修改VIA头域值
                            request.removeHeader(Via.NAME);
                            request.addHeader(via);
                        }


                        ResponseMsgUtil.sendMessage(to.getAddress().getURI().toString(), request.toString(), sipOptions);
//            RePlayCallTime.callPeriodic(request.toString(), to.getAddress().getURI().toString());

                        PorcessHandler.getBranchs().put(via.getBranch(), request.getFrom().getAddress().getURI().toString());//加入會畫管理branch
                        PorcessHandler.getTransactions().put(callID.getCallIdentifer().getLocalId(), via.getBranch());//加入會畫管理
                    } catch (Exception e){
                        logger.error(e.getMessage(), e);
                    }
                } else {
                    notExistsUser(request, sipOptions);
                }
            }
        });
    }


    /**
     * @Description 用戶不存在
     * @author zhang bo
     * @date 18-8-16
     * @version 1.0
     */
    public void notExistsUser(SIPRequest request, SipOptions sipOptions) {
        //回复404 用户不存在
        try {
            Response response = msgFactory.createResponse(Response.NOT_FOUND, request);
            ResponseMsgUtil.sendMessage(request.getFrom().getAddress().getURI().toString()
                    , response.toString(), sipOptions);
        } catch (ParseException e) {
            logger.error(e.getMessage(), e);
        }
    }
}
