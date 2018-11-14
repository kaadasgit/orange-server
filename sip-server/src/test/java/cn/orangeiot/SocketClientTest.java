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
//            logger.error(e.getMessage(), e)();
//        }

        String sendStr = "REGISTER sip:sip-kaadas.juziwulian.com SIP/2.0\r\n" +
                "Via: SIP/2.0/UDP 192.168.1.100:56860;branch=z9hG4bK.wmiGgi3Jt;rport\n" +
                "From: <sip:123@192.168.1.100>;tag=hZzBnIxXr\n" +
                "To: sip:123@192.168.1.100\n" +
                "CSeq: 20 REGISTER\n" +
                "Call-ID: bDMChfDXg-\n" +
                "Max-Forwards: 70\n" +
                "Supported: replaces, outbound\n" +
                "Accept: application/sdp\n" +
                "Accept: text/plain\n" +
                "Accept: application/vnd.gsma.rcs-ft-http+xml\n" +
                "Contact: <sip:5bc08b9110fcbb5693b6de97@192.168.168.247:56860;transport=udp>;+sip.instance=\"<urn:uuid:5cc80786-6fc0-4f3d-90ab-c49a9cf79676>\"\n" +
                "Expires: 10800\n" +
                "User-Agent: 凯迪仕智能_iOS11.3.1/3.16-122-g79a8bb2 (belle-sip/1.6.3)\n" +
                "\n";
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

        String receSend = "INVITE sip:null@192.168.1.101:54420;transport=udp SIP/2.0\r\n" +
                "From: <sip:null@192.168.1.100>;tag=VH6NLjo2Y\n" +
                "To: <sip:123@192.168.1.100>\n" +
                "CSeq: 20 INVITE\n" +
                "Call-ID: btXBuXBgee\n" +
                "Max-Forwards: 70\n" +
                "Supported: replaces,outbound\n" +
                "Allow: INVITE,ACK,CANCEL,OPTIONS,BYE,REFER,NOTIFY,MESSAGE,SUBSCRIBE,INFO,UPDATE\n" +
                "Content-Type: application/sdp\n" +
                "User-Agent: LinphoneAndroid/3.3.1 (belle-sip/1.6.3)\n" +
                "Contact: <sip:sipServer@192.168.1.105:5061>;expires=3600\n" +
                "Via: SIP/2.0/UDP 192.168.1.101:54420;branch=z9hG4bK.1ALZzEepy;rport\n" +
                "Content-Length: 492\n" +
                "\n" +
                "v=0\n" +
                "o=null 216 711 IN IP4 192.168.1.101\n" +
                "s=Talk\n" +
                "c=IN IP4 192.168.1.101\n" +
                "t=0 0\n" +
                "a=rtcp-xr:rcvr-rtt=all:10000 stat-summary=loss,dup,jitt,TTL voip-metrics\n" +
                "m=audio 7076 RTP/AVP 96 97 98 0 8 18 101 99 100\n" +
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
                "a=rtcp-fb:* ccm tmmbr";

        String sdp = "\r\n\r\n";

        Vertx vertx = Vertx.vertx();
        DatagramSocket socket = vertx.createDatagramSocket(new DatagramSocketOptions());

        Buffer buffer = Buffer.buffer(sendStr);
        socket.send(buffer, 5061, "127.0.0.1", asyncResult -> {
            System.out.println("Send succeeded? " + asyncResult.succeeded());
        });

        Buffer bufferHeart = Buffer.buffer().appendByte((byte) 13).appendByte((byte) 10).appendInt(10800).appendString("123");
        vertx.setPeriodic(30000, timeId -> {
            socket.send(bufferHeart, 5061, "127.0.0.1", asyncResult -> {
                System.out.println("Send succeeded? " + asyncResult.succeeded());
            });
        });

        socket.handler(datagramPacket->{
            System.out.println(new String(datagramPacket.data().toString()));
        });


//        } catch (SocketException e) {
//            logger.error(e.getMessage(), e)();
//        } catch (UnknownHostException e) {
//            logger.error(e.getMessage(), e)();
//        } catch (IOException e) {
//            logger.error(e.getMessage(), e)();
//        } finally {
//            // 关闭socket
//            if (datagramSocket != null) {
//                datagramSocket.close();
//            }
//        }
    }
}
