package cn.orangeiot.sip.proto.codec;

import gov.nist.javax.sip.SIPConstants;
import gov.nist.javax.sip.header.RequestLine;
import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.header.StatusLine;
import gov.nist.javax.sip.message.SIPMessage;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import gov.nist.javax.sip.parser.HeaderParser;
import gov.nist.javax.sip.parser.ParserFactory;
import gov.nist.javax.sip.parser.RequestLineParser;
import gov.nist.javax.sip.parser.StatusLineParser;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Objects;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-01-31
 */
public class MsgParserDecode {

    private static Logger logger = LogManager.getLogger(MsgParserDecode.class);

    /**
     * @Description 消息請求解码
     * @author zhang bo
     * @date 18-1-31
     * @version 1.0
     */
    public static void parseSIPMessage(byte[] msgBuffer, boolean readBody, boolean strict, Handler<AsyncResult<SIPMessage>> handler) {
        if (Objects.nonNull(msgBuffer) && msgBuffer.length > 0) {
            if (msgBuffer.length != 4 && !Arrays.toString(msgBuffer).equals("[13, 10, 13, 10]")) {
                int i = 0;
                // 開頭控制字符(0x20 空格)
                while (msgBuffer[i] < 0x20)
                    i++;
                String currentLine = null;//當前的一行data
                String currentHeader = null;//當前的頭部信息
                boolean isFirstLine = true;//是否是第一行
                SIPMessage message = null;//消息解析成消息對象

                //todo 讀取數據
                do {
                    int lineStart = i;
                    // 找到一行的長度 length
                    while (msgBuffer[i] != '\r' && msgBuffer[i] != '\n')
                        i++;

                    int lineLength = i - lineStart;

                    //轉成字符串
                    try {
                        currentLine = new String(msgBuffer, lineStart, lineLength, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        handler.handle(Future.failedFuture(e.getCause()));
                    }

                    // 獲取line string
                    currentLine = trimEndOfLine(currentLine);//去除尾部空格

                    if (currentLine.length() == 0) {
                        // Last header line, process the previous buffered header.
                        if (currentHeader != null && message != null) {
                            try {
                                processHeader(currentHeader, message);
                            } catch (ParseException e) {
                                handler.handle(Future.failedFuture(e.getCause()));
                            }
                        }

                    } else {
                        if (isFirstLine) {
                            try {
                                message = processFirstLine(currentLine, msgBuffer);
                            } catch (ParseException e) {
                                handler.handle(Future.failedFuture(e.getCause()));
                            }
                        } else {
                            char firstChar = currentLine.charAt(0);
                            if (firstChar == '\t' || firstChar == ' ') {
                                if (currentHeader == null)

                                    // This is a continuation, append it to the previous line.
                                    currentHeader += currentLine.substring(1);
                            } else {
                                if (currentHeader != null && message != null) {
                                    try {
                                        processHeader(currentHeader, message);
                                    } catch (ParseException e) {
                                        handler.handle(Future.failedFuture(e.getCause()));
                                    }
                                }
                                currentHeader = currentLine;
                            }
                        }
                    }

                    if (msgBuffer[i] == '\r' && msgBuffer.length > i + 1 && msgBuffer[i + 1] == '\n')
                        i++;

                    i++;

                    isFirstLine = false;
                } while (currentLine.length() > 0); //end
                if (message == null) handler.handle(Future.failedFuture("Bad message"));
                message.setSize(i);

                // Check for content legth header
                if (readBody && message.getContentLength() != null) {
                    if (message.getContentLength().getContentLength() != 0) {
                        int bodyLength = msgBuffer.length - i;

                        byte[] body = new byte[bodyLength];
                        System.arraycopy(msgBuffer, i, body, 0, bodyLength);
                        try {
                            message.setMessageContent(body, !strict, false, message.getContentLength().getContentLength());
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    } else if (!false && message.getContentLength().getContentLength() == 0 & strict) {
                        String last4Chars = new String(msgBuffer, msgBuffer.length - 4, 4);
                        if (!"\r\n\r\n".equals(last4Chars)) {
                            handler.handle(Future.failedFuture("Extraneous characters at the end of the message"));
                        }
                    }
                }
                handler.handle(Future.succeededFuture(message));
            } else {
                handler.handle(Future.failedFuture("=========The heartbeat packets"));
            }
        } else {
            handler.handle(Future.failedFuture("=========data msgBuffer is null"));
        }
    }

    /**
     * @Description 去除字符串尾部空格
     * @author zhang bo
     * @date 18-1-31
     * @version 1.0
     */
    protected static String trimEndOfLine(String line) {
        if (line == null)
            return line;

        int i = line.length() - 1;
        while (i >= 0 && line.charAt(i) <= 0x20)
            i--;

        if (i == line.length() - 1)
            return line;

        if (i == -1)
            return "";

        return line.substring(0, i + 1);
    }


    /**
     * @Description 处理第一行
     * @author zhang bo
     * @date 18-1-31
     * @version 1.0
     */
    protected static SIPMessage processFirstLine(String firstLine, byte[] msgBuffer) throws ParseException {
        SIPMessage message;
        if (!firstLine.startsWith(SIPConstants.SIP_VERSION_STRING)) {
            message = new SIPRequest();
            RequestLine requestLine = new RequestLineParser(firstLine + "\n")
                    .parse();
            ((SIPRequest) message).setRequestLine(requestLine);
        } else {
            message = new SIPResponse();
            StatusLine sl = new StatusLineParser(firstLine + "\n").parse();
            ((SIPResponse) message).setStatusLine(sl);
        }
        return message;
    }


    /**
     * @Description 处理头部
     * @author zhang bo
     * @date 18-1-31
     * @version 1.0
     */
    protected static void processHeader(String header, SIPMessage message) throws ParseException {
        if (header == null || header.length() == 0)
            return;

        HeaderParser headerParser = null;
        headerParser = ParserFactory.createParser(header + "\n");

        SIPHeader sipHeader = headerParser.parse();
        message.attachHeader(sipHeader, false);
    }
}
