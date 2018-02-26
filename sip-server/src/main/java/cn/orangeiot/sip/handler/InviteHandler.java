package cn.orangeiot.sip.handler;

import cn.orangeiot.sip.constant.SipOptions;
import cn.orangeiot.sip.message.ResponseMsgUtil;
import cn.orangeiot.sip.proto.codec.MsgParserDecode;
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

    private Map<String, URI> currUser;

    public InviteHandler(Map<String, Object> pool, MessageFactory msgFactory
            , HeaderFactory headerFactory, JsonObject jsonObject, AddressFactory addressFactory, Map<String, URI> currUser) {
        this.pool = pool;
        this.msgFactory = msgFactory;
        this.headerFactory = headerFactory;
        this.jsonObject = jsonObject;
        this.addressFactory = addressFactory;
        this.currUser = currUser;
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

        //查询目标地址
        String uri = request.getRequestURI().toString();
        String reqUri = uri.matches("^(sip):\\w+@[0-9.]+:[\\s\\S]*") ? uri.substring(0, uri.lastIndexOf(":")) : uri;
        URI contactURI = currUser.get(reqUri);
        logger.info("processInvite rqStr=" + reqUri + " contact=" + contactURI);

        //根据Request uri来路由，后续的响应消息通过VIA来路由
        MsgParserDecode.parseSIPMessage(request.toString().getBytes(), true, false, rs -> {
            if (rs.failed()) {
                rs.cause().printStackTrace();
            } else {
                try {
                    Request cliReq = (SIPRequest) rs.result();
                    cliReq.setRequestURI(contactURI);

                    Via callerVia = (Via) request.getHeader(Via.NAME);
                    Via via = (Via) headerFactory.createViaHeader(jsonObject.getString("host"), jsonObject.getInteger("port")
                            , sipOptions.toString(), callerVia.getBranch());

                    // FIXME 需要测试是否能够通过设置VIA头域来修改VIA头域值
                    cliReq.removeHeader(Via.NAME);
                    cliReq.addHeader(via);

                    // 更新contact的地址
                    ContactHeader contactHeader = headerFactory.createContactHeader();
                    Address address = addressFactory.createAddress("sip:sipsoft@" + jsonObject.getString("host") + ":" + jsonObject.getInteger("port"));
                    contactHeader.setAddress(address);
                    contactHeader.setExpires(3600);
                    cliReq.setHeader(contactHeader);

                    ResponseMsgUtil.sendMessage(reqUri.toString(), cliReq.toString(), sipOptions);


                    PorcessHandler.getTransactions().put(request.getTransactionId(), reqUri.toString());//加入會畫管理
                    PorcessHandler.getTransactions().put(via.getBranch(), request.getFrom().getAddress().getURI().toString());//加入會畫管理
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
