package cn.orangeiot;

import io.netty.util.CharsetUtil;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.impl.clustered.ClusteredMessage;
import io.vertx.core.eventbus.impl.codecs.JsonObjectMessageCodec;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.*;
import io.vertx.core.net.impl.ServerID;

import java.util.List;
import java.util.Map;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-05-24
 */
public class CodeTest {

    public static void main(String[] args) {
        ClusteredMessage message = new ClusteredMessage(
                new ServerID(12345, "localhost"), "cn.orangeiot.reg.user.UserAddremailCode"
                , "f2169ec7-b3c7-4afb-a588-a1046e071549", null,
                new JsonObject().put("mail", "qwe@123.com")
                        .put("versionType", "PHILIPS"),
                new JsonObjectMessageCodec(), true, null);
        Buffer sendMsg = message.encodeToWire();
//        System.out.println(data.toString());
//        System.out.println(bytesToHexFun1(data.getBytes()));

        CodeTest codeTest = new CodeTest();
        Buffer packMsg = codeTest.encodeToWire();

        Vertx vertx = Vertx.vertx();

        NetServer server = vertx.createNetServer(new NetServerOptions()
                .setReceiveBufferSize(1000000)
                .setIdleTimeout(2000).setLogActivity(true));

        NetClient client = vertx.createNetClient(new NetClientOptions()
                .setReceiveBufferSize(1000000)
                .setIdleTimeout(2000).setLogActivity(true));


        client.connect(33333, "127.0.0.1", rs -> {
            if (rs.failed()) {
                rs.cause().printStackTrace();
            } else {
                NetSocket socket = rs.result();
                socket.write(packMsg);
            }
        });


        server.connectHandler(netSocket -> {
//            netSocket.write(packMsg);

            netSocket.handler(data -> {
                System.out.println("SERVER received remoteAddress: " + netSocket.remoteAddress());
                System.out.println("SERVER I received some bytes: " + data.length());
                System.out.println("SERVER body(string):\n " + new String(data.getBytes()));
            });
        });//连接处理

        server.listen(12345, "127.0.0.1", res -> {
            if (res.succeeded()) {
                System.out.println("Server is now listening!");
            } else {
                res.cause().printStackTrace();
                System.err.println("Failed to bind!");
                System.exit(0);
            }
        });
    }


    public Buffer encodeToWire() {
        int length = 1024; // TODO make this configurable
        Buffer buffer = Buffer.buffer(length);
        buffer.appendInt(0);

        buffer.appendByte((byte) 1);

        byte systemCodecID = 13;
        buffer.appendByte(systemCodecID);

        buffer.appendByte(true ? (byte) 0 : (byte) 1);

        writeString(buffer, "cn.orangeiot.reg.user.UserAddremailCode");
        if ("f2169ec7-b3c7-4afb-a588-a1046e071549" != null) {
            writeString(buffer, "f2169ec7-b3c7-4afb-a588-a1046e071549");
        } else {
            buffer.appendInt(0);
        }
        buffer.appendInt(12345);
        writeString(buffer, "localhost");
        encodeHeaders(buffer);
        encodeToWire(buffer, new JsonObject().put("mail", "qwe@123.com")
                .put("versionType", "PHILIPS"));

        buffer.setInt(0, buffer.length() - 4);

        return buffer;
    }


    private void encodeHeaders(Buffer buffer) {
        MultiMap headers = null;
        if (headers != null && !headers.isEmpty()) {
            int headersLengthPos = buffer.length();
            buffer.appendInt(0);
            buffer.appendInt(headers.size());
            List<Map.Entry<String, String>> entries = headers.entries();
            for (Map.Entry<String, String> entry : entries) {
                writeString(buffer, entry.getKey());
                writeString(buffer, entry.getValue());
            }
            int headersEndPos = buffer.length();
            buffer.setInt(headersLengthPos, headersEndPos - headersLengthPos);
        } else {
            buffer.appendInt(4);
        }
    }


    private void writeString(Buffer buff, String str) {
        byte[] strBytes = str.getBytes(CharsetUtil.UTF_8);
        buff.appendInt(strBytes.length);
        buff.appendBytes(strBytes);
    }


    public void encodeToWire(Buffer buffer, JsonObject jsonObject) {
        Buffer encoded = jsonObject.toBuffer();
        buffer.appendInt(encoded.length());
        buffer.appendBuffer(encoded);
    }
}
