package cn.orangeiot.sip.handler;

import cn.orangeiot.sip.constant.SipOptions;
import cn.orangeiot.sip.message.ResponseMsgUtil;
import cn.orangeiot.sip.proto.codec.MsgParserDecode;
import gov.nist.javax.sip.header.CallID;
import gov.nist.javax.sip.header.To;
import gov.nist.javax.sip.header.Via;
import gov.nist.javax.sip.message.SIPRequest;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.URI;
import javax.sip.header.ContactHeader;
import javax.sip.header.HeaderFactory;
import javax.sip.header.ToHeader;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.text.ParseException;
import java.util.Map;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-02-02
 */
public class InviteHandler {

    private Map<String, Object> pool;

    private static Logger logger = LogManager.getLogger(InviteHandler.class);

    private MessageFactory msgFactory;

    private HeaderFactory headerFactory;

    private JsonObject jsonObject;

    private AddressFactory addressFactory;

    public InviteHandler(Map<String, Object> pool, MessageFactory msgFactory
            , HeaderFactory headerFactory, JsonObject jsonObject, AddressFactory addressFactory) {
        this.pool = pool;
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
    public void processInvite(SIPRequest request, SipOptions sipOptions) {
        try {
            //回复100 Trying
            Response response = msgFactory.createResponse(Response.TRYING, request);
            ResponseMsgUtil.sendMessage(request.getFrom().getAddress().getURI().toString()
                    , response.toString(), sipOptions);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        //根据Request uri来路由，后续的响应消息通过VIA来路由
        MsgParserDecode.parseSIPMessage(request.toString().getBytes(), true, false, rs -> {
            if (rs.failed()) {
                rs.cause().printStackTrace();
            } else {
                try {
                    //查找目標地址
                    Request cliReq = (SIPRequest) rs.result();
                    To to = (To) cliReq.getHeader(To.NAME);
                    ContactHeader contactHeader = (ContactHeader) request.getHeader("Contact");
                    Address contactAddr = contactHeader.getAddress();
                    URI contactURI = contactAddr.getURI();
                    logger.info("processInvite contact=" + contactURI);

                    cliReq.setRequestURI(contactURI);

                    Via callerVia = (Via) request.getHeader(Via.NAME);
                    Via via = (Via) headerFactory.createViaHeader(jsonObject.getString("host"), jsonObject.getInteger("port")
                            , sipOptions.toString(), callerVia.getBranch());

                    // FIXME 需要测试是否能够通过设置VIA头域来修改VIA头域值
                    cliReq.removeHeader(Via.NAME);
                    cliReq.addHeader(via);

                    // 更新contact的地址
                    ContactHeader contactHeaders = headerFactory.createContactHeader();
                    Address address = addressFactory.createAddress("sip:sipsoft@" + jsonObject.getString("host") + ":" + jsonObject.getInteger("port"));
                    contactHeaders.setAddress(address);
                    contactHeaders.setExpires(3600);
                    cliReq.setHeader(contactHeaders);

                    ResponseMsgUtil.sendMessage(to.getAddress().getURI().toString(), cliReq.toString(), sipOptions);

                    PorcessHandler.getBranchs().put(via.getBranch(), request.getFrom().getAddress().getURI().toString());//加入會畫管理branch
                    CallID callID=(CallID)request.getHeader(CallID.NAME);
                    PorcessHandler.getTransactions().put(callID.getCallIdentifer().getLocalId(), via.getBranch());//加入會畫管理
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
