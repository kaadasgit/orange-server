package cn.orangeiot.com.hankcs.network;

import io.vertx.core.Vertx;
import io.vertx.core.datagram.DatagramSocketOptions;
import io.vertx.core.json.JsonObject;
import org.ice4j.stack.StunServerTransaction;

import java.util.concurrent.atomic.AtomicInteger;


public class Peer {
    public static void main(String[] args) throws Throwable {
        try {
            Vertx vertx = Vertx.vertx();
            io.vertx.core.datagram.DatagramSocket socket = vertx.createDatagramSocket(new DatagramSocketOptions()
                    .setReuseAddress(true));

            String bindRequest ="[Response In: 6]\n" +
                    "Message Type: Binding Request (0x0001)\n" +
                    "Message Length: 0x0008\n" +
                    "Message Transaction ID: 016dac016686ea581213b4283d0ba104\n" +
                    "Attributes\n" +
                    "Attribute: CHANGE-REQUEST\n";


            socket.listen(8888, "192.168.42.8", asyncResult -> {
                if (asyncResult.succeeded()) {
                    socket.handler(rs -> {
                        System.out.println("rece data -> " + new String(rs.data().getBytes()));
                    });//数据包处理

                    socket.send(bindRequest, 3479, "114.67.58.242", as -> {
                        System.out.println("send data -> " + as.succeeded());
                        if (!as.succeeded()) {
                            reSend(vertx, bindRequest, 14500, "114.67.58.242", socket);
                        }
                    });
//                    try {
//                        String msg = new JsonObject().put("host", md.getConnection().getAddressType())
//                                .put("port", md.getMedia().getMediaPort()).toString();
//                        socket.send(msg, 14500, "114.67.58.243", as -> {
//                            System.out.println("send data -> " + as.succeeded());
//                            if (!as.succeeded()) {
//                                reSend(vertx, msg, 14500, "114.67.58.243", socket);
//                            }
//                        });
//                    } catch (Exception e) {
//                        logger.error(e.getMessage(), e)();
//                    }
                } else {
                    System.out.println("Listen failed" + asyncResult.cause());
                    System.out.println("Failed to bind!");
                    System.exit(0);
                }
            });

        } catch (Exception e) {
//            logger.error(e.getMessage(), e);
        }

    }


    /**
     * @param msg 消息
     * @Description 重发消息
     * @author zhang bo
     * @date 18-2-27
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public static void reSend(Vertx vertx, String msg, int port, String address, io.vertx.core.datagram.DatagramSocket socket) {
        // 重发消息
        AtomicInteger atomicInteger = new AtomicInteger(0);
        vertx.setPeriodic(1000, rs -> {
            atomicInteger.getAndIncrement();//原子自增
            if (atomicInteger.intValue() == 5) {//达到重发次数
                vertx.cancelTimer(rs);//取消周期定时
            }

            socket.send(msg, port, address, ars -> {
                if (ars.failed())
                    ars.cause().printStackTrace();
                else
                    vertx.cancelTimer(rs);//取消周期定时
            });
        });

    }
}