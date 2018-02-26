package cn.orangeiot;

import javax.net.SocketFactory;
import java.io.*;
import java.net.*;
import java.util.Date;

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

        String sendStr = "REGISTER sip:192.168.6.87 SIP/2.0\n" +
                "Via: SIP/2.0/UDP 192.168.6.87:47931;branch=z9hG4bK.kGwYvrj6D;rport\n" +
                "From: <sip:99@192.168.6.87>;tag=IWSzKJ4Hc\n" +
                "To: sip:99@192.168.6.87\n" +
                "CSeq: 20 REGISTER\n" +
                "Call-ID: eFWI4mgelF\n" +
                "Max-Forwards: 70\n" +
                "Supported: replaces, outbound\n" +
                "Accept: application/sdp\n" +
                "Accept: text/plain\n" +
                "Accept: application/vnd.gsma.rcs-ft-http+xml\n" +
                "Contact: <sip:99@172.16.1.4:47931;transport=udp>;+sip.instance=\"<urn:uuid:056fedbe-94ea-4606-a1a2-c2bcc0e28e55>\"\n" +
                "Expires: 3600\n" +
                "User-Agent: LinphoneAndroid/1.0 (belle-sip/1.5.0)\n" +
                "\n";
        String netAddress = "172.16.1.87";
        final int PORT_NUM = 5061;
        DatagramSocket datagramSocket = null;
        DatagramPacket datagramPacket = null;
        try {

            /*** 发送数据***/
            // 初始化datagramSocket,注意与前面Server端实现的差别
            datagramSocket = new DatagramSocket(47931);
            // 使用DatagramPacket(byte buf[], int length, InetAddress address, int port)函数组装发送UDP数据报
            byte[] buf = sendStr.getBytes();
            InetAddress address = InetAddress.getByName(netAddress);
            datagramPacket = new DatagramPacket(buf, buf.length, address, PORT_NUM);
            // 发送数据
            datagramSocket.send(datagramPacket);

            /*** 接收数据***/
            byte[] receBuf = new byte[1024];
            DatagramPacket recePacket = new DatagramPacket(receBuf, receBuf.length);
            datagramSocket.receive(recePacket);

            String receStr = new String(recePacket.getData(), 0, recePacket.getLength());
            System.out.println("Client Rece Ack:" + receStr);
            System.out.println(recePacket.getPort());
            System.out.println(recePacket.getAddress());
            datagramSocket.close();

        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // 关闭socket
            if (datagramSocket != null) {
                datagramSocket.close();
            }
        }
    }
}
