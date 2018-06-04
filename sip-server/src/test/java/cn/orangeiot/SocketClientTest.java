package cn.orangeiot;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.datagram.DatagramSocket;
import io.vertx.core.datagram.DatagramSocketOptions;


/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-02-05
 */
public class SocketClientTest {
    public static void main(String[] args) {
//        try {
//            Socket socket = new Socket("192.168.6.87", 5061);//创建一个客户端连接
//            OutputStream out = socket.getOutputStream();//获取服务端的输出流，为了向服务端输出数据
//            InputStream in = socket.getInputStream();//获取服务端的输入流，为了获取服务端输入的数据
//
//            PrintWriter bufw = new PrintWriter(out, true);
//            BufferedReader bufr = new BufferedReader(new InputStreamReader(in));
//            bufw.write("ACK sip:sipsoft@192.168.6.87:5061 SIP/2.0\n" +
//                    "Via: SIP/2.0/UDP 192.168.6.46:49022;rport;branch=z9hG4bK.lm0~kvxga\n" +
//                    "From: <sip:40@192.168.6.87>;tag=F-ufThj61\n" +
//                    "To: <sip:45@192.168.6.87>;tag=dRqNxhS\n" +
//                    "CSeq: 20 ACK\n" +
//                    "Call-ID: NF1qRcY2ea\n" +
//                    "Max-Forwards: 70\n" +
//                    "User-Agent: LinphoneAndroid/1.0 (belle-sip/1.5.0)\n" +
//                    "\n");
//            bufw.flush();
//            String info = null;
//            while ((info = bufr.readLine()) != null) {//循环读取客户端的信息
//                System.out.println("我是客户端，服务器说：" + info);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        String sendStr = "INVITE sip:zwz222@192.168.1.100 SIP/2.0\n" +
                "Via: SIP/2.0/UDP 192.168.1.100:52744;branch=z9hG4bK.pgb17jekJ;rport\n" +
                "Via: SIP/2.0/UDP 192.168.1.100:52743;branch=z9hDFbK.erb17jekJ;rport\n" +
                "From: <sip:18680369651@192.168.1.100>;tag=wbfj8Om30\n" +
                "To: \"zwz222\" <sip:zwz222@192.168.1.100>\n" +
                "CSeq: 20 INVITE\n" +
                "Call-ID: assi2b5a24\n" +
                "Max-Forwards: 70\n" +
                "Supported: replaces, outbound\n" +
                "Allow: INVITE, ACK, CANCEL, OPTIONS, BYE, REFER, NOTIFY, MESSAGE, SUBSCRIBE, INFO, UPDATE\n" +
                "Content-Type: application/sdp\n" +
                "Content-Length: 1363\n" +
                "Contact: <sip:18680369651@192.168.1.102:54685;transport=udp>;+sip.instance=\"<urn:uuid:56300f5f-3a7e-45a1-b784-c68a7b8e51a8>\"\n" +
                "User-Agent: Linphone_iPhone10.3_iOS11.2.6/3.16.5 (belle-sip/1.6.3)\n" +
                "\n" +
                "v=0\n" +
                "o=18680369651 134 3691 IN IP4 192.168.1.102\n" +
                "s=Talk\n" +
                "c=IN IP4 192.168.1.102\n" +
                "t=0 0\n" +
                "a=ice-pwd:d4ac7a39ec4b0b2e1297ee1f\n" +
                "a=ice-ufrag:76e745bb\n" +
                "a=rtcp-xr:rcvr-rtt=all:10000 stat-summary=loss,dup,jitt,TTL voip-metrics\n" +
                "m=audio 55244 RTP/AVP 96 97 98 0 8 18 101 99 100\n" +
                "c=IN IP4 183.15.178.246\n" +
                "a=rtpmap:96 opus/48000/2\n" +
                "a=fmtp:96 useinbandfec=1\n" +
                "a=rtpmap:97 speex/16000\n" +
                "a=fmtp:97 vbr=on\n" +
                "a=rtpmap:98 speex/8000\n" +
                "a=fmtp:98 vbr=on\n" +
                "a=fmtp:18 annexb=yes\n" +
                "a=rtpmap:101 telephone-event/48000\n" +
                "a=rtpmap:99 telephone-event/16000\n" +
                "a=rtpmap:100 telephone-event/8000\n" +
                "a=rtcp:55244\n" +
                "a=candidate:1 1 UDP 2130706431 240.0.2.1 7216 typ host\n" +
                "a=candidate:1 2 UDP 2130706430 240.0.2.1 7217 typ host\n" +
                "a=candidate:2 1 UDP 1694498815 183.15.178.246 55244 typ srflx raddr 240.0.2.1 rport 7216\n" +
                "a=candidate:2 2 UDP 1694498814 183.15.178.246 55244 typ srflx raddr 240.0.2.1 rport 7217\n" +
                "a=rtcp-fb:* trr-int 5000\n" +
                "a=rtcp-fb:* ccm tmmbr\n" +
                "m=video 55244 RTP/AVP 96\n" +
                "c=IN IP4 183.15.178.246\n" +
                "a=rtpmap:96 H264/90000\n" +
                "a=fmtp:96 profile-level-id=42801F\n" +
                "a=rtcp:55244\n" +
                "a=candidate:1 1 UDP 2130706431 240.0.2.1 9206 typ host\n" +
                "a=candidate:1 2 UDP 2130706430 240.0.2.1 9207 typ host\n" +
                "a=candidate:2 1 UDP 1694498815 183.15.178.246 55244 typ srflx raddr 240.0.2.1 rport 9206\n" +
                "a=candidate:2 2 UDP 1694498814 183.15.178.246 55244 typ srflx raddr 240.0.2.1 rport 9207\n" +
                "a=rtcp-fb:* trr-int 5000\n" +
                "a=rtcp-fb:* ccm tmmbr\n" +
                "a=rtcp-fb:96 nack pli\n" +
                "a=rtcp-fb:96 H264/90000\n" +
                "a=fmtp:96 profile-level-id=42801F";
//        String netAddress = "192.168.1.100";
//        final int PORT_NUM = 5061;
//        DatagramSocket datagramSocket = null;
//        DatagramPacket datagramPacket = null;
//        try {

//
//            /*** 发送数据***/
//            // 初始化datagramSocket,注意与前面Server端实现的差别
//            datagramSocket = new DatagramSocket(15897);
//            // 使用DatagramPacket(byte buf[], int length, InetAddress address, int port)函数组装发送UDP数据报
//            byte[] buf = sendStr.getBytes();
//            InetAddress address = InetAddress.getByName(netAddress);
//            datagramPacket = new DatagramPacket(buf, buf.length, address, PORT_NUM);
//            // 发送数据
//            for (int i = 0; i < 10; i ++) {
//                datagramSocket.send(datagramPacket);
//            }

//            /*** 接收数据***/
//            byte[] receBuf = new byte[1024];
//            DatagramPacket recePacket = new DatagramPacket(receBuf, receBuf.length);
//            datagramSocket.receive(recePacket);
//
//            String receStr = new String(recePacket.getData(), 0, recePacket.getLength());
//            System.out.println("Client Rece Ack:" + receStr);
//            System.out.println(recePacket.getPort());
//            System.out.println(recePacket.getAddress());
//            datagramSocket.close();


        Vertx vertx = Vertx.vertx();
        DatagramSocket socket = vertx.createDatagramSocket(new DatagramSocketOptions());
        Buffer buffer = Buffer.buffer(sendStr);
        socket.send(buffer, 5061, "47.106.87.6", asyncResult -> {
            System.out.println("Send succeeded? " + asyncResult.succeeded());
        });

//        } catch (SocketException e) {
//            e.printStackTrace();
//        } catch (UnknownHostException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            // 关闭socket
//            if (datagramSocket != null) {
//                datagramSocket.close();
//            }
//        }
    }
}
