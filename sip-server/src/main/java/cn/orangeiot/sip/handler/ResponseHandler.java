package cn.orangeiot.sip.handler;

import cn.orangeiot.sip.constant.SipOptions;
import cn.orangeiot.sip.message.ResponseMsgUtil;
import gov.nist.javax.sip.header.ContentLength;
import gov.nist.javax.sip.header.ContentType;
import gov.nist.javax.sip.header.MaxForwards;
import gov.nist.javax.sip.header.Via;
import gov.nist.javax.sip.message.SIPResponse;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
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
public class ResponseHandler {

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

        if (code == Response.TRYING) {
            logger.info("The response is 100 response.");
            return;
        }


        Response callerResp = null;
        Via via = (Via) response.getHeader(Via.NAME);
        String uri = PorcessHandler.getBranchs().get(via.getBranch());
        try {
            callerResp = msgFactory.createResponse(code, response.getCallId(),
                    response.getCSeq(), response.getFrom(), response.getTo()
                    , response.getViaHeaders(), new MaxForwards(70));

            // 更新contact头域值，因为后面的消息是根据该URI来路由的
            ContactHeader contactHeader = headerFactory.createContactHeader();
            Address address = addressFactory.createAddress("sip:sipsoft@" + jsonObject.getString("host") + ":" + jsonObject.getInteger("port"));
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


        // 拷贝相应的消息体
        ContentLength contentLen = (ContentLength) response.getContentLength();
        if (contentLen != null && contentLen.getContentLength() != 0) {
            ContentType contentType = (ContentType) response.getHeader(ContentType.NAME);
            System.out.println("the sdp contenttype is " + contentType);

            callerResp.setContentLength(contentLen);
            //callerResp.addHeader(contentType);
            try {
                callerResp.setContent(response.getContent(), contentType);

            } catch (ParseException e) {
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
