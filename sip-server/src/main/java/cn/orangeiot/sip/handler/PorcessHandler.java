package cn.orangeiot.sip.handler;

import cn.orangeiot.reg.user.UserAddr;
import cn.orangeiot.sip.constant.SipOptions;
import cn.orangeiot.sip.message.ResponseMsgUtil;
import cn.orangeiot.sip.proto.codec.MsgParserDecode;
import gov.nist.core.NameValueList;
import gov.nist.javax.sip.Utils;
import gov.nist.javax.sip.header.*;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import io.vertx.core.Vertx;
import io.vertx.core.datagram.DatagramPacket;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.SocketAddress;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sip.InvalidArgumentException;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.URI;
import javax.sip.header.*;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.text.ParseException;
import java.util.*;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-01-30
 */
public class PorcessHandler implements UserAddr {

    private static Logger logger = LogManager.getLogger(PorcessHandler.class);

    private final RegisterHandler registerHandler;

    private final InviteHandler inviteHandler;

    private final ResponseHandler responseHandler;

    private static Map<String, String> branchs = new HashMap<>();//保存会话branch

    private static Map<String, String> transactions = new HashMap<>();//保存會畫

    private MessageFactory msgFactory;

    private HeaderFactory headerFactory;

    private AddressFactory addressFactory;

    private Vertx vertx;

    private JsonObject jsonObject;

    public PorcessHandler(MessageFactory msgFactory, HeaderFactory headerFactory, JsonObject jsonObject
            , AddressFactory addressFactory, Vertx vertx) {
        this.registerHandler = new RegisterHandler(msgFactory, jsonObject);
        this.inviteHandler = new InviteHandler(msgFactory, headerFactory, jsonObject, addressFactory);
        this.responseHandler = new ResponseHandler(msgFactory, headerFactory, addressFactory, jsonObject);
        this.msgFactory = msgFactory;
        this.headerFactory = headerFactory;
        this.addressFactory = addressFactory;
        this.vertx = vertx;
        this.jsonObject = jsonObject;
    }


    /**
     * @Description 獲取會畫事務
     * @author zhang bo
     * @date 18-2-2
     * @version 1.0
     */
    public static Map<String, String> getTransactions() {
        return transactions;
    }


    /**
     * @Description 获取会话branch
     * @author zhang bo
     * @date 18-2-28
     * @version 1.0
     */
    public static Map<String, String> getBranchs() {
        return branchs;
    }

    /**
     * 服务处理tcp
     */
    public void processTcp(NetSocket netSocket) {

        netSocket.handler(buffer -> {
            logger.debug("SERVER received remoteAddress -> {} , bytes -> {}", netSocket.remoteAddress(), buffer.length());
            logger.debug("SERVER body(string):\n " + new String(buffer.getBytes()));

            // 消息协议解析
            MsgParserDecode.parseSIPMessage(buffer.getBytes(), true, false, rs -> {
                if (rs.failed()) {//不是sip标准协议
                    logger.error(rs.cause().getMessage(), rs.cause());
                    netSocket.close();
                } else {
                    if (Objects.nonNull(rs.result())) {
                        if (rs.result() instanceof SIPResponse)
                            responseSwitch((SIPResponse) rs.result(), SipOptions.TCP, netSocket.remoteAddress());//回包
                        else
                            redirectSwitch((SIPRequest) rs.result(), netSocket, SipOptions.TCP, null);//转发处理
                    } else {//心跳包
                        vertx.eventBus().send(UserAddr.class.getName() + HEARTBEAT_REGISTER_USER,
                                new JsonObject().put("socketAddress", netSocket.remoteAddress().toString())
                                        .put("expires", jsonObject.getInteger("heartIdleTime")));
                    }
                }
            });
        });
        //end
        netSocket.endHandler(socket -> {
            logger.info("=====end====");
            netSocket.close();
        });
        //close
        netSocket.closeHandler(socket -> {
            logger.info("=====close====");
            netSocket.close();
        });
        //exception
        netSocket.exceptionHandler(socket -> {
            logger.info("=====exception====");
            netSocket.close();
        });
    }

    /**
     * 服务处理udp
     */
    @SuppressWarnings("Duplicates")
    public void processUdp(DatagramPacket datagramPacket) {
        logger.debug("SERVER received remoteAddress -> {} , bytes -> {}", datagramPacket.sender().host()
                , datagramPacket.data().length());
        logger.debug("SERVER body(string):\n " + new String(datagramPacket.data().getBytes()));

        // 消息协议解析
        MsgParserDecode.parseSIPMessage(datagramPacket.data().getBytes(), true, false, rs -> {
            if (rs.failed()) {//不是sip标准协议
                logger.error(rs.cause().getMessage(), rs.cause());
            } else {
                if (Objects.nonNull(rs.result())) {
                    if (rs.result() instanceof SIPResponse)
                        responseSwitch((SIPResponse) rs.result(), SipOptions.UDP, datagramPacket.sender());//回包
                    else
                        redirectSwitch((SIPRequest) rs.result(), null, SipOptions.UDP, datagramPacket.sender());//转发处理
                } else {//心跳包
                    vertx.eventBus().send(UserAddr.class.getName() + HEARTBEAT_REGISTER_USER,
                            new JsonObject().put("socketAddress", datagramPacket.sender().toString())
                                    .put("expires", jsonObject.getInteger("heartIdleTime")));
                }
            }
        });
    }


    /**
     * @Description 转发处理
     * @author zhang bo
     * @date 18-1-31
     * @version 1.0
     */

    public void redirectSwitch(SIPRequest sipMessage, NetSocket netSocket, SipOptions sipOptions, SocketAddress socketAddress) {
        if (!Objects.nonNull(sipMessage)) {
            logger.error("processRequest request is null.");
            return;
        }
        logger.info("request fristline -> {} , method -> {}", sipMessage.getFirstLine(), sipMessage.getMethod());
        switch (sipMessage.getMethod()) {
            case Request.REGISTER://注冊處理
                registerHandler.processRegister(sipMessage, sipOptions, socketAddress, vertx);
                break;
            case Request.INVITE://invite請求
                inviteHandler.processInvite(sipMessage, sipOptions, vertx);
                break;
            case Request.ACK://ACK
                this.processAck(sipMessage, sipOptions);
                break;
            case Request.SUBSCRIBE://订阅
                this.processSubscribe(sipMessage, sipOptions);
                break;
            case Request.CANCEL://断开请求
                this.processCancel(sipMessage, sipOptions);
                break;
            case Request.BYE://呼叫释放
                this.processBye(sipMessage, sipMessage.getViaHeaders(), sipOptions);
                break;
            default:
                logger.error("PorcessHandler==redirectSwitch   no support the method!");
                break;
        }
    }


    /**
     * 处理CANCEL请求
     *
     * @param request 请求消息
     */
    @SuppressWarnings("Duplicates")
    private void processCancel(Request request, SipOptions sipOptions) {
        try {
            // 发送CANCEL 200 OK消息
            To to = (To) request.getHeader(To.NAME);
            From from = (From) request.getHeader(From.NAME);
            Response response = msgFactory.createResponse(Response.OK, request);
            ResponseMsgUtil.sendMessage(from.getAddress().getURI().toString(), response.toString(), sipOptions);

            // 向对端发送CANCEL消息
            Request cancelReq = null;
            List list = new ArrayList();
            Via viaHeader = (Via) request.getHeader(Via.NAME);
            list.add(viaHeader);

            CSeq cseq = (CSeq) request.getHeader(CSeq.NAME);
            CSeq cancelCSeq = (CSeq) headerFactory.createCSeqHeader(cseq.getSeqNumber(), Request.CANCEL);
            cancelReq = msgFactory.createRequest(request.getRequestURI(),
                    request.getMethod(),
                    (CallIdHeader) request.getHeader(CallIdHeader.NAME),
                    cancelCSeq,
                    (FromHeader) request.getHeader(From.NAME),
                    (ToHeader) request.getHeader(ToHeader.NAME),
                    list,
                    (MaxForwardsHeader) request.getHeader(MaxForwardsHeader.NAME));
            ResponseMsgUtil.sendMessage(to.getAddress().getURI().toString(), cancelReq.toString(), sipOptions);

            //回收废数据
            CallID callID = (CallID) request.getHeader(CallID.NAME);
            branchs.remove(transactions.get(callID.getCallIdentifer().toString()));
            transactions.remove(callID.getCallIdentifer().toString());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

    }

    /**
     * 处理BYE请求
     *
     * @param request 请求消息
     */
    @SuppressWarnings("Duplicates")
    private void processBye(Request request, ViaList vias, SipOptions sipOptions) {
        Request byeReq = null;
        try {
            To to = (To) request.getHeader(To.NAME);
            byeReq = msgFactory.createRequest(request.toString());
            // 拷贝相应的消息体
            ContentLength contentLen = (ContentLength) request.getContentLength();
            if (contentLen != null && contentLen.getContentLength() != 0) {
                ContentType contentType = (ContentType) request.getHeader(ContentType.NAME);
                byeReq.setContentLength(contentLen);
                try {
                    byeReq.setContent(request.getContent(), contentType);

                } catch (ParseException e) {
                    logger.error(e.getMessage(), e);
                }
            } else {
                logger.info("sdp is null");
            }
            ResponseMsgUtil.sendMessage(to.getAddress().getURI().toString(), byeReq.toString(), sipOptions);

            //回收废数据
            CallID callID = (CallID) request.getHeader(CallID.NAME);
            branchs.remove(transactions.get(callID.getCallIdentifer().toString()));
            transactions.remove(callID.getCallIdentifer().toString());
        } catch (Exception e) {
            //  Auto-generated catch block
            logger.error(e.getMessage(), e);
        }

    }


    /**
     * 处理SUBSCRIBE请求
     *
     * @param request 请求消息
     */
    private void processSubscribe(Request request, SipOptions sipOptions) {
        try {
            ToHeader head = (ToHeader) request.getHeader(ToHeader.NAME);
            Address toAddress = head.getAddress();
            URI toURI = toAddress.getURI();
            Response response = null;
            response = msgFactory.createResponse(200, request);
            if (response != null) {
                ExpiresHeader expireHeader = headerFactory.createExpiresHeader(30);
                response.setExpires(expireHeader);
            }
            ResponseMsgUtil.sendMessage(toURI.toString(), response.toString(), sipOptions);

        } catch (ParseException e) {
            //  Auto-generated catch block
            logger.error(e.getMessage(), e);
        } catch (InvalidArgumentException e) {
            //  Auto-generated catch block
            logger.error(e.getMessage(), e);
        }
    }


    /**
     * @Description 回包
     * @author zhang bo
     * @date 18-2-2
     * @version 1.0
     */
    public void responseSwitch(SIPResponse response, SipOptions sipOptions, SocketAddress socketAddress) {
        responseHandler.processResponse(response, sipOptions, socketAddress);
    }


    /**
     * 处理ACK请求
     *
     * @param request 请求消息
     */
    @SuppressWarnings("Duplicates")
    private void processAck(Request request, SipOptions sipOptions) {
        try {
            SIPRequest sipRequest = new SIPRequest();
            sipRequest.setMethod(Request.ACK);
            ToHeader head = (ToHeader) request.getHeader(ToHeader.NAME);
            Address toAddress = head.getAddress();
            URI toURI = toAddress.getURI();
            sipRequest.setRequestURI(toURI);
            sipRequest.setCallId((CallID) request.getHeader(CallID.NAME));
            sipRequest.setCSeq((CSeq) request.getHeader(CSeq.NAME));
            List<Via> vias = new ArrayList<>();

            Via via = (Via) request.getHeader(Via.NAME);
            via.removeParameters();
            if (request != null && via != null) {
                NameValueList originalRequestParameters = via.getParameters();
                if (originalRequestParameters != null
                        && originalRequestParameters.size() > 0) {
                    via.setParameters((NameValueList) originalRequestParameters
                            .clone());
                }
            }
            via.setBranch(Utils.getInstance().generateBranchId()); // new branch
            vias.add(via);
            sipRequest.setVia(vias);
//            sipRequest.addHeader(via);
            sipRequest.setFrom((From) request.getHeader(From.NAME));
            sipRequest.setTo((To) request.getHeader(To.NAME));
            sipRequest.setMaxForwards((MaxForwards) request.getHeader(MaxForwards.NAME));

            ResponseMsgUtil.sendMessage(toURI.toString(), sipRequest.toString(), sipOptions);
        } catch (Exception e) {
            //  Auto-generated catch block
            logger.error(e.getMessage(), e);
        }
    }


}
