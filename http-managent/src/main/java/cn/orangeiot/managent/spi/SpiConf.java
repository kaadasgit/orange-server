package cn.orangeiot.managent.spi;

import cn.orangeiot.common.constant.HttpAttrType;
import cn.orangeiot.managent.handler.BaseHandler;
import cn.orangeiot.managent.handler.device.PublishDeviceHandler;
import cn.orangeiot.managent.handler.memenet.MemeNetHandler;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.ext.web.Router;
import io.vertx.spi.cluster.zookeeper.ZookeeperClusterManager;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-12-08
 */
public class SpiConf {
    private static Logger logger = LogManager.getLogger(SpiConf.class);

    private JsonObject configJson;

    private Vertx vertx;

    private Router router;

    private JsonObject vertxConfig;

    public SpiConf(JsonObject vertxConfig) {
        this.vertxConfig = vertxConfig;
    }

    /**
     * 加载api
     */
    public void uploadApi(AsyncResult<Vertx> res) {
        if (res.succeeded()) {
            vertx = res.result();
            router = Router.router(vertx);


            /**
             * 通用配置
             */
            BaseHandler baseHandler = new BaseHandler(vertx, configJson);
            baseHandler.enableCorsSupport(router);
            baseHandler.bodyOrUpload(router);
            baseHandler.globalIntercept(router);
            baseHandler.produces(router);


            PublishDeviceHandler publishDeviceHandler = new PublishDeviceHandler(vertx.eventBus(), configJson);
            router.get(ApiConf.PRODUCTION_DEVICESN).blockingHandler(publishDeviceHandler::productionDeviceSN);
            router.get(ApiConf.PRODUCTION_MODELSN).blockingHandler(publishDeviceHandler::productionBLESN);
            router.post(ApiConf.UPLOAD_MODEL_MAC).blockingHandler(publishDeviceHandler::uploadMacAddr);

            MemeNetHandler memeNetHandler = new MemeNetHandler(vertx.eventBus(), configJson);
            router.get(ApiConf.REGISTER_USER_BULK).produces(HttpAttrType.CONTENT_TYPE_JSON.getValue()).blockingHandler(memeNetHandler::onRegisterUserBulk);

            createHttpServerManagent();//创建httpServer后台管理
        } else {
            // failed!
            logger.error(res.cause().getMessage(), res.cause());
        }
    }


    /**
     * @Description 创建http服务器
     * @author zhang bo
     * @date 17-12-8
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void createHttpServerManagent() {
        InputStream jksIn = SpiConf.class.getResourceAsStream("/server.jks");
        Buffer buffer = null;
        try {
            byte[] jksByte = IOUtils.toByteArray(jksIn);
            buffer = Buffer.buffer().appendBytes(jksByte);
            vertx.createHttpServer(
                    new HttpServerOptions().setCompressionSupported(true).setSsl(true)
                            .setKeyStoreOptions(new JksOptions().setValue(buffer)
                                    .setPassword(configJson.getString("pwd"))))
                    .requestHandler(router::accept).listen(vertxConfig.getInteger("http-port",
                    configJson.getInteger("port")),
                    vertxConfig.getString("host-name", configJson.getString("host")));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @Description
     * @author zhang bo
     * @date 17-12-8
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void loadClusting() {
        InputStream zkIn = SpiConf.class.getResourceAsStream("/zkConf.json");
        InputStream configIn = SpiConf.class.getResourceAsStream("/config.json");//全局配置
        String zkConf = "";//jdbc连接配置
        String config = "";
        try {
            zkConf = IOUtils.toString(zkIn, "UTF-8");//获取配置
            config = IOUtils.toString(configIn, "UTF-8");

            if (!zkConf.equals("")) {
                JsonObject json = new JsonObject(zkConf);
                configJson = new JsonObject(config);

                System.setProperty("vertx.zookeeper.hosts", json.getString("hosts.zookeeper"));
                ClusterManager mgr = new ZookeeperClusterManager(json);
                VertxOptions options = new VertxOptions().setClusterManager(mgr);
//                options.setClusterHost(configJson.getString("host"));//本机地址

                //集群
                Vertx.clusteredVertx(options, this::uploadApi);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public Router getRouter() {
        return router;
    }

    public void setRouter(Router router) {
        this.router = router;
    }

    public Vertx getVertx() {
        return vertx;
    }

    public void setVertx(Vertx vertx) {
        this.vertx = vertx;
    }

    public JsonObject getConfigJson() {
        return configJson;
    }

    public void setConfigJson(JsonObject configJson) {
        this.configJson = configJson;
    }
}
