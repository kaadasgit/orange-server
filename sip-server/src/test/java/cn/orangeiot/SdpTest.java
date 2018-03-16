package cn.orangeiot;

import gov.nist.core.NameValue;
import gov.nist.javax.sdp.SessionDescriptionImpl;
import gov.nist.javax.sdp.fields.AttributeField;
import gov.nist.javax.sdp.parser.SDPAnnounceParser;
import org.ice4j.ice.sdp.CandidateAttribute;

import javax.sdp.Attribute;
import javax.sdp.MediaDescription;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.text.ParseException;
import java.util.Objects;
import java.util.Vector;

import static junit.framework.Assert.assertNotNull;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-03-08
 */
public class SdpTest {


    public static void main(String[] args) {
//        String sdpData[] = {
//
//                "\r\n " + "v=0\r\n" + "o=4855 13760799956958020 13760799956958020" + " IN IP4 129.6.55.78\r\n" + "s=mysession session\r\n"
//                        + "p=+46 8 52018010\r\n" + "c=IN IP4 129.6.55.78\r\n" + "t=0 0\r\n" + "m=audio 6022 RTP/AVP 0 4 18\r\n"
//                        + "a=rtpmap:0 PCMU/8000\r\n" + "a=rtpmap:4 G723/8000\r\n" + "a=rtpmap:18 G729A/8000\r\n" +
//                        "a=ptime:20\r\n" + "u=http://www.example.com/seminars/sdp.pdf\r\n",
//
//                "v=0\r\n" + "o=root 14539767 1208 IN IP4 66.237.65.67\r\n" + "s=session\r\n"
//                        + "t=0 0\r\n" + "m=audio 38658 RTP/AVP 3 110 97 0 8 101\r\n" + "c=IN IP4 66.237.65.67\r\n" +
//                        "a=rtpmap:3 GSM/8000\r\n" + "a=rtpmap:110 speex/8000\r\n"
//                        + "a=rtpmap:97 iLBC/8000\r\n" +
//                        "a=rtpmap:0 PCMU/8000\r\n" + "a=rtpmap:8 PCMA/8000\r\n"
//                        + "a=rtpmap:101 telephone-event/8000\r\n" + "a=fmtp:101 0-16\r\n" + "a=silenceSupp:off - - - -\r\n"
//                        + "u=http://www.example.com/seminars/sdp.pdf\r\n",
//
//                "v=0\r\n" + "o=Cisco-SIPUA 10163 1 IN IP4 192.168.0.103\r\n" + "s=SIP Call\r\n"
//                        + "t=0 0\r\n" + "m=audio 27866 RTP/AVP 0 8 18 101\r\n" + "c=IN IP4 192.168.0.103\r\n" +
//                        "a=rtpmap:0 PCMU/8000\r\n" + "a=rtpmap:8 PCMA/8000\r\n" + "a=rtpmap:18 G729/8000\r\n" +
//                        "a=fmtp:18 annexb=no\r\n" + "a=rtpmap:101 telephone-event/8000\r\n"
//                        + "a=fmtp:101 0-15\r\n" + "a=sendonly\r\n"
//                        + "u=http://www.example.com/seminars/sdp.pdf\r\n",
//
//                "v=0\r\n" + "o=- 1167770389 1167770390 IN IP4 192.168.5.242\r\n"
//                        + "s=Polycom IP Phone\r\n" + "c=IN IP4 192.168.5.242\r\n" + "t=0 0\r\n"
//                        + "a=sendonly\r\n" + "m=audio 2222 RTP/AVP 0 101\r\n"
//                        + "a=rtpmap:0 PCMU/8000\r\n" + "a=rtpmap:101 telephone-event/8000\r\n"
//                        + "u=http://www.example.com/seminars/sdp.pdf\r\n"
//
//
//
//
//
//        };


        String sdp = "v=0\r\n" +
                "o=789 2607 4062 IN IP4 192.168.137.208\r\n" +
                "s=Talk\r\n" +
                "c=IN IP4 192.168.137.208\r\n" +
                "t=0 0\r\n" +
                "a=rtcp-xr:rcvr-rtt=all:10000 stat-summary=loss,dup,jitt,TTL voip-metrics\r\n" +
                "m=audio 7076 RTP/AVP 96 97 98 0 8 18 99 101 100 102\r\n" +
                "a=rtpmap:96 opus/48000/2\r\n" +
                "a=fmtp:96 useinbandfec=1\r\n" +
                "a=rtpmap:97 speex/16000\r\n" +
                "a=fmtp:97 vbr=on\r\n" +
                "a=rtpmap:98 speex/8000\r\n" +
                "a=fmtp:98 vbr=on\r\n" +
                "a=fmtp:18 annexb=yes\r\n" +
                "a=rtpmap:99 iSAC/16000\r\n" +
                "a=rtpmap:101 telephone-event/48000\r\n" +
                "a=rtpmap:100 telephone-event/16000\r\n" +
                "a=rtpmap:102 telephone-event/8000\r\n" +
                "a=candidate:1 1 UDP 2130706431 192.168.1.108 7296 typ host\r\n" +
                "a=candidate:1 2 UDP 2130706430 192.168.1.108 7297 typ host\r\n" +
                "a=candidate:2 1 UDP 1694498815 116.24.67.36 7296 typ srflx raddr 192.168.1.108 rport 7296\r\n" +
                "a=candidate:2 2 UDP 1694498814 116.24.67.36 7297 typ srflx raddr 192.168.1.108 rport 7297\r\n" +
                "a=rtcp-fb:* trr-int 5000\r\n" +
                "a=rtcp-fb:* ccm tmmbr";

//        for (String sdpdata : sdpData) {
        SDPAnnounceParser parser = new SDPAnnounceParser(sdp);
        SessionDescriptionImpl parsedDescription = null;
        try {
            parsedDescription = parser.parse();
            SessionDescriptionImpl sessiondescription = new SessionDescriptionImpl(parsedDescription);
            Vector attrs = sessiondescription.getAttributes(false);

            if (attrs != null) {
                Attribute attrib = (Attribute) attrs.get(0);
                System.out.println("attrs = " + attrib.getName());
            }
            MediaDescription md = (MediaDescription) sessiondescription.getMediaDescriptions(
                    false).get(0);
            md.getMedia().getMediaPort();
            parsedDescription.getConnection().getAddress();

            Object cc = md.getAttributes(false).stream().filter(r -> r.toString().startsWith("a=candidate"))
                    .filter(r -> r.toString().startsWith("a=candidate:2")).findFirst().orElseGet(null);
            if (Objects.nonNull(cc)) {
                String[] address = cc.toString().split("\\s+");
                String host = address[4];
                String port = address[5];
                System.out.println("host:"+host+"::port:"+port);
            }

            System.out.println("md attributes " + md.getAttributes(false));

            SessionDescriptionImpl sessiondescription1 = new SDPAnnounceParser(sessiondescription
                    .toString()).parse();

            System.out.println("sessionDescription1 " + sessiondescription1);

            // Unfortunately equals is not yet implemented.
            // assertEquals("Equality check",
            // sessiondescription,sessiondescription1);

            // Check if SDP is serializable
            File outFile = File.createTempFile("sdpObj", ".dat");
            outFile.deleteOnExit();
            ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(outFile, false));
            os.writeObject(sessiondescription1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
//    }
}
