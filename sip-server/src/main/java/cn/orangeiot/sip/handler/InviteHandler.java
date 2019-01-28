package cn.orangeiot.sip.handler;

import cn.orangeiot.common.constant.NotifyConf;
import cn.orangeiot.common.options.SendOptions;
import cn.orangeiot.reg.message.MessageAddr;
import cn.orangeiot.reg.user.UserAddr;
import cn.orangeiot.sip.constant.SipOptions;
import cn.orangeiot.sip.message.ResponseMsgUtil;
import gov.nist.javax.sip.header.CallID;
import gov.nist.javax.sip.header.To;
import gov.nist.javax.sip.header.Via;
import gov.nist.javax.sip.header.ViaList;
import gov.nist.javax.sip.message.SIPRequest;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sip.InvalidArgumentException;
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
public class InviteHandler implements UserAddr, MessageAddr {

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
     * @Description 获取应用推送处理通知
     * @author zhang bo
     * @date 18-11-9
     * @version 1.0
     */
    public void getPushIdAndProcessNotify(Vertx vertx, String uid, Handler<Boolean> handler) {
        if (uid == null || uid.length() < 1)
            handler.handle(false);
        vertx.eventBus().send(MessageAddr.class.getName() + GET_PUSHID, new JsonObject().put("uid", uid), SendOptions.getInstance(), res -> {
            if (res.failed()) {
                logger.error(res.cause().getMessage(), res);
                handler.handle(false);
            } else {
                if (res.result() != null && res.result().body() != null) {
                    handler.handle(true);
                } else
                    handler.handle(false);
            }
        });
    }


    /**
     * @Description 回复成功
     * @author zhang bo
     * @date 18-11-9
     * @version 1.0
     */
    public void replySuccess(SIPRequest request, SipOptions sipOptions) {
        //回复100 Trying
        try {
            SipURI uri = (SipURI) request.getFrom().getAddress().getURI();
            String uid = uri.getUser();
            Response response = msgFactory.createResponse(Response.TRYING, request);
            ResponseMsgUtil.sendMessage(request.getFrom().getAddress().getURI().toString()
                    , response.toString(), sipOptions, uid);
        } catch (ParseException e) {
            logger.error(e.getMessage(), e);
        }
    }


    /**
     * @Description 包装请求
     * @author zhang bo
     * @date 18-11-9
     * @version 1.0
     */
    public SIPRequest warpRequest(SIPRequest request, SipOptions sipOptions, To to, Via via) {
        ContactHeader contactHeader = (ContactHeader) request.getHeader("Contact");
        Address contactAddr = contactHeader.getAddress();

        //根据Request uri来路由，后续的响应消息通过VIA来路由
        URI contactURI = contactAddr.getURI();
        logger.debug("processInvite contact=" + contactURI);
        request.setRequestURI(contactURI);


        // 更新contact的地址
        ContactHeader contactHeaders = headerFactory.createContactHeader();
        Address address = null;
        try {
            address = addressFactory.createAddress("sip:sipServer@" + jsonObject.getString("host") + ":" + jsonObject.getInteger("port"));
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
        } catch (ParseException e) {
            logger.error(e.getMessage(), e);
        } catch (InvalidArgumentException e) {
            logger.error(e.getMessage(), e);
        }
        return request;
    }

    /**
     * @Description invite請求處理
     * @author zhang bo
     * @date 18-2-2
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void processInvite(SIPRequest request, SipOptions sipOptions, Vertx vertx) {
        //查询是否存在相关账户信息
        To to = (To) request.getHeader(To.NAME);
        Via via = (Via) request.getHeader(Via.NAME);

        SipURI uri = (SipURI) to.getAddress().getURI();
        String uid = uri.getUser();

        //转发对应客户端
        CallID callID = (CallID) request.getHeader(CallID.NAME);

        SIPRequest newRequest = warpRequest(request, sipOptions, to, via);

        SipURI fromUri = (SipURI) newRequest.getFrom().getAddress().getURI();
        String fromUid = fromUri.getUser();

        vertx.eventBus().send(UserAddr.class.getName() + SAVE_SESSION_BRANCH, new JsonObject().put("branch"
                , callID.getCallIdentifer().toString()).put("uid", fromUid).put("expire", jsonObject.getInteger("branchExpire")));//加入會畫管理branch

        vertx.eventBus().send(UserAddr.class.getName() + GET_REGISTER_USER, uid, SendOptions.getInstance(), rs -> {
            if (rs.failed()) {
                logger.error(rs.cause().getMessage(), rs);
                notExistsUser(request, sipOptions);
            } else {
                logger.info("---------------------------1;");
                if (Objects.nonNull(rs.result().body())) {
                    //回复100 Trying
                    replySuccess(request, sipOptions);

                    ResponseMsgUtil.sendMessage(to.getAddress().getURI().toString(), newRequest.toString(), sipOptions, uid);
                } else {
                    logger.info("-------------------------2;");
                    getPushIdAndProcessNotify(vertx, uid, flag -> {
                        if (!flag) {
                            notExistsUser(request, sipOptions);
                        } else {
                            logger.info("----------------------------3;");
                            vertx.eventBus().send(MessageAddr.class.getName() + BRANCH_SEND_TIMES, new JsonObject().put("callId", callID.getCallIdentifer().toString())
                                    .put("deviceId", fromUid), SendOptions.getInstance(), (AsyncResult<Message<JsonObject>> times) -> {
                                if (times.failed()) {
                                    logger.error(times.cause());
                                } else {
                                    logger.info("----------------------------4");
                                    if (times.result() != null && times.result().body() != null) {
//                                        vertx.eventBus().send(MessageAddr.class.getName() + SEND_APPLICATION_SOUND_NOTIFY, new JsonObject().put("uid", uid)
//                                                .put("title", NotifyConf.CAT_EYE_TITLE).put("content", NotifyConf.CAT_EYE_CONTERNT).put("extras", new JsonObject()
//                                                        .put("func", "catEyeCall").put("gwId", times.result().body().getString("deviceSN")).put("deviceId", fromUid)
//                                                        .put("data", newRequest.toString()))
//                                                .put("time_to_live", 30));
//
//                                        replySuccess(request, sipOptions);
                                    }else{
                                        logger.info("------------------------times.result   is   null");
                                    }
                                    vertx.eventBus().send(MessageAddr.class.getName() + SEND_APPLICATION_SOUND_NOTIFY, new JsonObject().put("uid", uid)
                                            .put("title", NotifyConf.CAT_EYE_TITLE).put("content", NotifyConf.CAT_EYE_CONTERNT).put("extras", new JsonObject()
                                                    .put("func", "catEyeCall").put("gwId", "1233211234567").put("deviceId", fromUid)
                                                    .put("data", newRequest.toString()))
                                            .put("time_to_live", 30));

                                    replySuccess(request, sipOptions);
                                }
                            });

                        }
                    });
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
            SipURI uri = (SipURI) request.getFrom().getAddress().getURI();
            String uid = uri.getUser();
            Response response = msgFactory.createResponse(Response.NOT_FOUND, request);
            ResponseMsgUtil.sendMessage(request.getFrom().getAddress().getURI().toString()
                    , response.toString(), sipOptions, uid);
        } catch (ParseException e) {
            logger.error(e.getMessage(), e);
        }
    }
}
