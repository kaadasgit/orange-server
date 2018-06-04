package cn.orangeiot.sip;

import cn.orangeiot.sip.constant.SipOptions;
import cn.orangeiot.sip.handler.PorcessHandler;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Log4JLoggerFactory;
import io.vertx.core.Vertx;
import io.vertx.core.datagram.DatagramSocket;
import io.vertx.core.datagram.DatagramSocketOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sip.address.AddressFactory;
import javax.sip.header.HeaderFactory;
import javax.sip.message.MessageFactory;
import java.util.Objects;


/**
 * @author zhang bo
 * @version 1.0
 * @Description sip实例
 * @date 2018-01-30
 */
public class SipVertxFactory {

    private static Logger logger = LogManager.getLogger(SipVertxFactory.class);
    private static SipVertxFactory myFactory = null;
    private String pathName = "gov.nist";

    private MessageFactory messageFactory = null;

    private HeaderFactory headerFactory = null;

    private AddressFactory addressFactory = null;

    private static DatagramSocket socket = null;

    private static Vertx vertx;

    private static JsonObject config;

    public static synchronized SipVertxFactory getInstance() {
        if (myFactory == null) {
            myFactory = new SipVertxFactory();
        }
        return myFactory;
    }

    public static Vertx getVertx() {
        return vertx;
    }

    public static JsonObject getConfig() {
        return config;
    }

    /**
     * @Description 获取udp实例
     * @author zhang bo
     * @date 18-2-7
     * @version 1.0
     */
    public static DatagramSocket getSocketInstance() {
        return socket;
    }


    /**
     * @param var3 协议
     * @Description 启动监听
     * @author zhang bo
     * @date 18-1-30
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void createListeningPoint(SipOptions var3, Vertx vertx, JsonObject jsonObject) {
        Objects.requireNonNull(var3);
        Objects.requireNonNull(jsonObject);
        this.vertx = vertx;
        this.config = jsonObject;
        switch (var3) {
            case TCP://TCP服务器
                InternalLoggerFactory.setDefaultFactory(Log4JLoggerFactory.INSTANCE);
                NetServer server = vertx.createNetServer(new NetServerOptions()
                        .setReceiveBufferSize(jsonObject.getInteger("receBufferSize"))
                        .setIdleTimeout(jsonObject.getInteger("idleTimeout")).setLogActivity(true));

                MessageFactory msgFactory = this.createMessageFactory();
                HeaderFactory headerFactory = this.createHeaderFactory();
                AddressFactory addressFactory = this.createAddressFactory();
                PorcessHandler porcessHandler = new PorcessHandler(msgFactory, headerFactory, jsonObject, addressFactory,vertx);

                server.connectHandler(porcessHandler::processTcp);//连接处理
                server.listen(jsonObject.getInteger("port"), jsonObject.getString("startHost"), res -> {
                    if (res.succeeded()) {
                        logger.info("Server is now listening!");
                    } else {
                        res.cause().printStackTrace();
                        logger.error("Failed to bind!");
                        System.exit(0);
                    }
                });
                break;
            case UDP://UDP服务器
                socket = vertx.createDatagramSocket(new DatagramSocketOptions()
                        .setReuseAddress(true).setReusePort(true));

                MessageFactory msgFactoryUdp = this.createMessageFactory();
                HeaderFactory headerFactoryUdp = this.createHeaderFactory();
                AddressFactory addressFactoryUdp = this.createAddressFactory();
                PorcessHandler porcessHandlerUdp = new PorcessHandler(msgFactoryUdp, headerFactoryUdp, jsonObject, addressFactoryUdp,vertx);

                socket.listen(jsonObject.getInteger("port"), jsonObject.getString("startHost"), asyncResult -> {
                    if (asyncResult.succeeded()) {
                        socket.handler(porcessHandlerUdp::processUdp);//数据包处理
                    } else {
                        logger.error("Listen failed" + asyncResult.cause());
                        logger.error("Failed to bind!");
                        System.exit(0);
                    }
                });
                break;
            default:
                logger.fatal("====not options , start fail");
                break;
        }
    }


    public HeaderFactory createHeaderFactory() {
        if (headerFactory == null) {
            headerFactory = (HeaderFactory) createSipFactory("javax.sip.header.HeaderFactoryImpl");
        }
        return headerFactory;
    }


    public AddressFactory createAddressFactory() {
        if (addressFactory == null) {
            addressFactory = (AddressFactory) createSipFactory("javax.sip.address.AddressFactoryImpl");
        }
        return addressFactory;
    }

    public MessageFactory createMessageFactory() {
        if (messageFactory == null) {
            messageFactory = (MessageFactory) createSipFactory("javax.sip.message.MessageFactoryImpl");
        }
        return messageFactory;
    }

    /**
     * @Description 所有创建方法使用的私有实用方法返回一个实例
     * @author zhang bo
     * @date 18-2-1
     * @version 1.0
     */
    private Object createSipFactory(String objectClassName) {

        // If the stackClassName is null, then throw an exception
        if (objectClassName == null) {
            return null;
        }
        try {
            Class peerObjectClass = Class.forName(getPathName() + "."
                    + objectClassName);
            // 创建一个新实例
            Object newPeerObject = peerObjectClass.newInstance();
            return (newPeerObject);
        } catch (Exception e) {
            String errmsg = "The Peer Factory: "
                    + getPathName()
                    + "."
                    + objectClassName
                    + " could not be instantiated. Ensure the Path Name has been set.";
            logger.error(errmsg);
        }
        return null;
    }

    public String getPathName() {
        return pathName;
    }

    public void setPathName(String pathName) {
        this.pathName = pathName;
    }
}
