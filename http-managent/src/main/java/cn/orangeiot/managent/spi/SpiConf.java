package cn.orangeiot.managent.spi;

import cn.orangeiot.common.annotation.http.HttpZkFactory;
import cn.orangeiot.common.constant.HttpAttrType;
import cn.orangeiot.managent.handler.BaseHandler;
import cn.orangeiot.managent.handler.device.PublishDeviceHandler;
import cn.orangeiot.managent.handler.memenet.MemeNetHandler;
import cn.orangeiot.managent.handler.ota.OTAHandler;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.ext.hawkular.AuthenticationOptions;
import io.vertx.ext.hawkular.VertxHawkularOptions;
import io.vertx.ext.web.Router;
import io.vertx.spi.cluster.zookeeper.ZookeeperClusterManager;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.MDC;
import org.apache.log4j.NDC;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.ThreadContext;
import org.apache.zookeeper.CreateMode;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

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

    /**
     * @author : baijun
     * @date : 2019-01-04
     * 集群对象
     */
    private ClusterManager clusterManager;

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
            baseHandler.exceptionAndTimeout(router);


            PublishDeviceHandler publishDeviceHandler = new PublishDeviceHandler(vertx, configJson);
            router.get(ApiConf.PRODUCTION_DEVICESN).blockingHandler(publishDeviceHandler::productionDeviceSN);
            router.get(ApiConf.PRODUCTION_MODELSN).blockingHandler(publishDeviceHandler::productionBLESN);
            router.post(ApiConf.UPLOAD_MODEL_MAC).handler(publishDeviceHandler::uploadMacAddr);
            router.post(ApiConf.UPLOAD_FILE_MAC).blockingHandler(publishDeviceHandler::uploadMacFile);
            router.post(ApiConf.UPLOAD_FILE_MAC_RESULT).produces(HttpAttrType.CONTENT_TYPE_JSON.getValue())
                    .handler(publishDeviceHandler::getWriteMacResult);
            router.post(ApiConf.UPLOAD_DEVICE_TEST_INFO).blockingHandler(publishDeviceHandler::uploadDeviceTestInfo);
            router.post(ApiConf.UPLOAD_DEVICE_BIND).blockingHandler(publishDeviceHandler::uploadDeviceBind);
            router.get(ApiConf.PRODUCT_TEST_USER).blockingHandler(publishDeviceHandler::productionTestUser);

            MemeNetHandler memeNetHandler = new MemeNetHandler(vertx.eventBus(), configJson);
            router.get(ApiConf.REGISTER_USER_BULK).produces(HttpAttrType.CONTENT_TYPE_JSON.getValue()).blockingHandler(memeNetHandler::onRegisterUserBulk);


            OTAHandler otaHandler = new OTAHandler(vertx, configJson);
            router.post(ApiConf.SELECT_MODEL).produces(HttpAttrType.CONTENT_TYPE_JSON.getValue()).handler(otaHandler::selectModelAll);
            router.post(ApiConf.SELECT_DATE_RANGE).produces(HttpAttrType.CONTENT_TYPE_JSON.getValue()).handler(otaHandler::selectDateRange);
            router.post(ApiConf.SELECT_NUM_RANGE).produces(HttpAttrType.CONTENT_TYPE_JSON.getValue()).handler(otaHandler::selectNumRange);
            router.post(ApiConf.SUBMIT_UPGRADE_DATA).produces(HttpAttrType.CONTENT_TYPE_JSON.getValue()).handler(otaHandler::submitOTAUpgrade);

            createHttpServerManagent();//创建httpServer后台管理

            /**
             * @date : 2019-01-04
             * @description : 把 http-managent 项目 http 信息存储到 zookeeper，临时节点
             */
            HttpZkFactory.Instance.handleHttpManagentMsg(configJson.getInteger("port"),"/zk/http-managent",ApiConf.class,((ZookeeperClusterManager)clusterManager).getCuratorFramework(), CreateMode.EPHEMERAL);
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
                                    .setPassword(configJson.getString("pwd")))
                            .setIdleTimeout(configJson.getInteger("IdleTimeout")))
                    .requestHandler(router::accept).listen(
                    configJson.getInteger("port"), configJson.getString("host"));
            ThreadContext.put("ip", configJson.getString("host"));
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
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

                if (Objects.nonNull(System.getProperty("CLUSTER")))
                    json.put("rootPath", System.getProperty("CLUSTER"));

                System.setProperty("vertx.zookeeper.hosts", json.getString("hosts.zookeeper"));
                ClusterManager mgr = new ZookeeperClusterManager(json);
                VertxOptions options = new VertxOptions().setClusterManager(mgr);

                clusterManager = mgr; // 为集群对象赋值
//                        .setMetricsOptions(new VertxHawkularOptions().setEnabled(true)
//                                .setHost("127.0.0.1")
//                                .setPort(8080)
//                                .setTenant("hawkular").setAuthenticationOptions(
//                                        new AuthenticationOptions()
//                                                .setEnabled(true)
//                                                .setId("test")
//                                                .setSecret("123456")
//                                ));

                if (Objects.nonNull(json.getValue("node.host")))
                    options.setClusterHost(json.getString("node.host"));

                //集群
                Vertx.clusteredVertx(options, this::uploadApi);
            }

        } catch (IOException e) {
            logger.error(e.getMessage(), e);
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
