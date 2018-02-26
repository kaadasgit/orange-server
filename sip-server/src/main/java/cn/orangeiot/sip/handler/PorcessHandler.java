package cn.orangeiot.sip.handler;

import cn.orangeiot.sip.SipVertxFactory;
import cn.orangeiot.sip.constant.SipOptions;
import cn.orangeiot.sip.message.ResponseMsgUtil;
import cn.orangeiot.sip.proto.codec.MsgParserDecode;
import gov.nist.javax.sip.header.*;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.datagram.DatagramPacket;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.SocketAddress;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sip.*;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.URI;
import javax.sip.header.ExpiresHeader;
import javax.sip.header.HeaderFactory;
import javax.sip.header.ToHeader;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-01-30
 */
public class PorcessHandler {

    private static Logger logger = LogManager.getLogger(PorcessHandler.class);

    private final RegisterHandler registerHandler;

    private final InviteHandler inviteHandler;

    private final ResponseHandler responseHandler;

    private static final Map<String, Object> pool = new HashMap<>();//保存当前注册的用户

    private static Map<String, URI> currUser = new HashMap<>();//保存用户打contact url

    private static Map<String, String> transactions = new HashMap<>();//保存會畫

    private MessageFactory msgFactory;

    private HeaderFactory headerFactory;

    private AddressFactory addressFactory;

    /**
     * 主叫对话
     */
    private Dialog calleeDialog = null;

    /**
     * 被叫对话
     */
    private Dialog callerDialog = null;


    public PorcessHandler(MessageFactory msgFactory, HeaderFactory headerFactory, JsonObject jsonObject
            , AddressFactory addressFactory) {
        this.registerHandler = new RegisterHandler(pool, msgFactory, currUser);
        this.inviteHandler = new InviteHandler(pool, msgFactory, headerFactory, jsonObject, addressFactory, currUser);
        this.responseHandler = new ResponseHandler(msgFactory, headerFactory, addressFactory, jsonObject);
        this.msgFactory = msgFactory;
        this.headerFactory = headerFactory;
        this.addressFactory = addressFactory;
    }


    /**
     * @Description 獲取連接集合
     * @author zhang bo
     * @date 18-2-2
     * @version 1.0
     */
    public static Map<String, Object> getNetSocketList() {
        return pool;
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
     * 服务处理tcp
     */
    public void processTcp(NetSocket netSocket) {
        netSocket.handler(buffer -> {
            logger.info("SERVER received remoteAddress: " + netSocket.remoteAddress());
            logger.info("SERVER I received some bytes: " + buffer.length());
            logger.info("SERVER body(string):\n " + new String(buffer.getBytes()));
            //todo 消息协议解析
            MsgParserDecode.parseSIPMessage(buffer.getBytes(), true, false, rs -> {
                if (rs.failed()) {//不是sip标准协议
                    rs.cause().printStackTrace();
                    netSocket.close();
                } else {
                    try {
                        responseSwitch((SIPResponse) rs.result(), SipOptions.TCP);//回包
                    } catch (ClassCastException e) {
                        redirectSwitch((SIPRequest) rs.result(), netSocket, SipOptions.TCP, null);//转发处理
                    }
                }
            });
        });
        //end
        netSocket.endHandler(socket -> {
            logger.info("=====end====");
            pool.remove(netSocket);
            netSocket.close();
        });
        //close
        netSocket.closeHandler(socket -> {
            logger.info("=====close====");
            pool.remove(netSocket);
            netSocket.close();
        });
        //exception
        netSocket.exceptionHandler(socket -> {
            logger.info("=====exception====");
            pool.remove(netSocket);
            netSocket.close();
        });
    }

    /**
     * 服务处理udp
     */
    @SuppressWarnings("Duplicates")
    public void processUdp(DatagramPacket datagramPacket) {
        logger.info("SERVER received remoteAddress: " + datagramPacket.sender().toString());
        logger.info("SERVER I received some bytes: " + datagramPacket.data().length());
        logger.info("SERVER body(string):\n " + new String(datagramPacket.data().getBytes()));

        //todo 消息协议解析
        MsgParserDecode.parseSIPMessage(datagramPacket.data().getBytes(), true, false, rs -> {
            if (rs.failed()) {//不是sip标准协议
                rs.cause().printStackTrace();
            } else {
                try {
                    responseSwitch((SIPResponse) rs.result(), SipOptions.UDP);//回包
                } catch (ClassCastException e) {
                    redirectSwitch((SIPRequest) rs.result(), null, SipOptions.UDP, datagramPacket.sender());//转发处理
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
        logger.info("==PorcessHandler==redirectSwitch===request fristline====" + sipMessage.getFirstLine());
        logger.info("==PorcessHandler==redirectSwitch===request method====" + sipMessage.getMethod());
        switch (sipMessage.getMethod()) {
            case Request.REGISTER://注冊處理
                registerHandler.processRegister(sipMessage, netSocket, sipOptions, socketAddress);
                break;
            case Request.INVITE://invite請求
                inviteHandler.processInvite(sipMessage, sipOptions);
                break;
            case Request.ACK://ACK
                this.processAck(sipMessage, sipOptions);
                break;
            case Request.SUBSCRIBE:
                logger.error("===========" + Request.SUBSCRIBE);
                this.processSubscribe(sipMessage, sipOptions);
                break;
            case Request.CANCEL:
                System.exit(0);
                logger.error("===========" + Request.CANCEL);
                break;
            case Request.BYE:
                this.processBye(sipMessage, sipMessage.getViaHeaders(), sipOptions);
                logger.error("===========" + Request.BYE);
                break;
            default:
                logger.error("no support the method!");
                break;
        }


//        if (Request.INVITE.equals(method)) {
//            registerHandler.processRegister(sipMessage);
//        } else if (Request.REGISTER.equals(method)) {
//            processRegister(request, arg0);
//        } else if (Request.SUBSCRIBE.equals(method)) {
//            processSubscribe(request);
//        } else if (Request.ACK.equalsIgnoreCase(method)) {
//            processAck(request, arg0);
//        } else if (Request.BYE.equalsIgnoreCase(method)) {
//            processBye(request, arg0);
//        } else if (Request.CANCEL.equalsIgnoreCase(method)) {
//            processCancel(request, arg0);
//        } else {
//            System.out.println("no support the method!");
//        }
    }


    /**
     * 处理BYE请求
     *
     * @param request 请求消息
     */
    @SuppressWarnings("Duplicates")
    private void processBye(Request request, ViaList vias, SipOptions sipOptions) {
        Response byeReq = null;
        try {
            Via via = (Via) request.getHeader(Via.NAME);
            String uri = PorcessHandler.getTransactions().get(via.getBranch());
            byeReq = msgFactory.createResponse(Response.OK, (CallID) request.getHeader(CallID.NAME),
                    (CSeq) request.getHeader(CSeq.NAME), (From) request.getHeader(From.NAME), (To) request.getHeader(To.NAME)
                    , vias, new MaxForwards(70));
            // 拷贝相应的消息体
            ContentLength contentLen = (ContentLength) request.getContentLength();
            if (contentLen != null && contentLen.getContentLength() != 0) {
                ContentType contentType = (ContentType) request.getHeader(ContentType.NAME);
                System.out.println("the sdp contenttype is " + contentType);
                byeReq.setContentLength(contentLen);
                try {
                    byeReq.setContent(request.getContent(), contentType);

                } catch (ParseException e) {
                    e.printStackTrace();
                }
            } else {
                logger.info("sdp is null.");
            }
            ResponseMsgUtil.sendMessage(uri, byeReq.toString(), sipOptions);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
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
            logger.info("response : " + response.toString());
            ResponseMsgUtil.sendMessage(toURI.toString(), response.toString(), sipOptions);

        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvalidArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    /**
     * @Description 回包
     * @author zhang bo
     * @date 18-2-2
     * @version 1.0
     */
    public void responseSwitch(SIPResponse response, SipOptions sipOptions) {
        responseHandler.processResponse(response, sipOptions);
    }


    /**
     * 处理ACK请求
     *
     * @param request 请求消息
     */
    @SuppressWarnings("Duplicates")
    private void processAck(Request request, SipOptions sipOptions) {
        try {
            Request ackRequest = null;
            CSeq csReq = (CSeq) request.getHeader(CSeq.NAME);
            ackRequest = calleeDialog.createAck(csReq.getSeqNumber());
            calleeDialog.sendAck(ackRequest);
            System.out.println("send ack to callee:" + ackRequest.toString());
        } catch (SipException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvalidArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


}
