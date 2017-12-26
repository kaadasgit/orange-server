package cn.orangeiot.http.spi;

import cn.orangeiot.common.constant.HttpAttrType;
import cn.orangeiot.http.handler.BaseHandler;
import cn.orangeiot.http.handler.file.FileHandler;
import cn.orangeiot.http.handler.lock.LockHandler;
import cn.orangeiot.http.handler.message.MessageHandler;
import cn.orangeiot.http.handler.user.UserHandler;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.*;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.ext.web.Router;
import io.vertx.spi.cluster.zookeeper.ZookeeperClusterManager;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-12-08
 */
public class SpiConf {
    private static Logger logger = LoggerFactory.getLogger(SpiConf.class);

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
            baseHandler.bodyOrUpload(router);
            baseHandler.enableCorsSupport(router);
            baseHandler.ExceptionAndTimeout(router);
            baseHandler.staticResource(router);
            baseHandler.requestlog(router);
            baseHandler.loadFilterUrl();
            baseHandler.produces(router);
            baseHandler.globalIntercept(router);


            //TODO 用户相关
            UserHandler userHandler = new UserHandler(vertx.eventBus(), configJson);
            router.post(ApiConf.USER_LOGIN_TEL).consumes(HttpAttrType.CONTENT_TYPE_JSON.getValue()).produces(HttpAttrType.CONTENT_TYPE_JSON.getValue()).handler(userHandler::getUserByTel);
            router.post(ApiConf.USER_LOGIN_MAIL).consumes(HttpAttrType.CONTENT_TYPE_JSON.getValue()).produces(HttpAttrType.CONTENT_TYPE_JSON.getValue()).handler(userHandler::getUserByEmail);
            router.post(ApiConf.USER_REGISTER_TEL).consumes(HttpAttrType.CONTENT_TYPE_JSON.getValue()).produces(HttpAttrType.CONTENT_TYPE_JSON.getValue()).handler(userHandler::registerUserByTel);
            router.post(ApiConf.USER_REGISTER_MAIL).consumes(HttpAttrType.CONTENT_TYPE_JSON.getValue()).produces(HttpAttrType.CONTENT_TYPE_JSON.getValue()).handler(userHandler::registerUserByMail);
            router.post(ApiConf.USER_NICKNAME).consumes(HttpAttrType.CONTENT_TYPE_JSON.getValue()).produces(HttpAttrType.CONTENT_TYPE_JSON.getValue()).handler(userHandler::getNickName);
            router.post(ApiConf.UPDATE_NICKNAME).consumes(HttpAttrType.CONTENT_TYPE_JSON.getValue()).produces(HttpAttrType.CONTENT_TYPE_JSON.getValue()).handler(userHandler::updateNickName);
            router.post(ApiConf.UPDATE_PASSWORD).consumes(HttpAttrType.CONTENT_TYPE_JSON.getValue()).produces(HttpAttrType.CONTENT_TYPE_JSON.getValue()).handler(userHandler::updateUserPwd);
            router.post(ApiConf.FORGET_PASSWORD).consumes(HttpAttrType.CONTENT_TYPE_JSON.getValue()).produces(HttpAttrType.CONTENT_TYPE_JSON.getValue()).handler(userHandler::forgetPwd);
            router.post(ApiConf.SUGGEST_MSG).consumes(HttpAttrType.CONTENT_TYPE_JSON.getValue()).produces(HttpAttrType.CONTENT_TYPE_JSON.getValue()).handler(userHandler::suggestMsg);
            router.post(ApiConf.USER_LOGOUT).produces(HttpAttrType.CONTENT_TYPE_JSON.getValue()).handler(userHandler::logOut);

            //TODO 消息相关
            MessageHandler messageHandler = new MessageHandler(vertx.eventBus(), configJson);
            router.post(ApiConf.SEND_SMS_CODE).consumes(HttpAttrType.CONTENT_TYPE_JSON.getValue()).produces(HttpAttrType.CONTENT_TYPE_JSON.getValue()).handler(messageHandler::sendSMS);
            router.post(ApiConf.SEND_EMAIL_CODE).consumes(HttpAttrType.CONTENT_TYPE_JSON.getValue()).produces(HttpAttrType.CONTENT_TYPE_JSON.getValue()).handler(messageHandler::sendMail);


            //TODO 文件相关
            FileHandler fileHandler = new FileHandler(vertx, configJson);
            router.get(ApiConf.GET_FILE_HEADER_IMG).handler(fileHandler::getHeaderImg);
            router.post(ApiConf.UPLOAD_HEADER_IMG).consumes(HttpAttrType.CONTENT_TYPE_FORM_DATA.getValue()).produces(HttpAttrType.CONTENT_TYPE_JSON.getValue()).handler(fileHandler::downHeaderImg);


            //TODO 锁相关
            LockHandler lockHandler=new LockHandler(vertx.eventBus(),configJson);
            router.post(ApiConf.CREATE_ADMIN_DEV).consumes(HttpAttrType.CONTENT_TYPE_JSON.getValue()).produces(HttpAttrType.CONTENT_TYPE_JSON.getValue()).handler(lockHandler::createAdminDev);
            router.post(ApiConf.DELETE_EVEND_DEV).consumes(HttpAttrType.CONTENT_TYPE_JSON.getValue()).produces(HttpAttrType.CONTENT_TYPE_JSON.getValue()).handler(lockHandler::deletevendorDev);
            router.post(ApiConf.DELETE_ADMIN_DEV).consumes(HttpAttrType.CONTENT_TYPE_JSON.getValue()).produces(HttpAttrType.CONTENT_TYPE_JSON.getValue()).handler(lockHandler::deleteAdminDev);
            router.post(ApiConf.DELETE_NORMAL_DEV).consumes(HttpAttrType.CONTENT_TYPE_JSON.getValue()).produces(HttpAttrType.CONTENT_TYPE_JSON.getValue()).handler(lockHandler::deleteNormalDev);
            router.post(ApiConf.CREATE_NORMAL_DEV).consumes(HttpAttrType.CONTENT_TYPE_JSON.getValue()).produces(HttpAttrType.CONTENT_TYPE_JSON.getValue()).handler(lockHandler::createNormalDev);
            router.post(ApiConf.GET_OPEN_LOCK_RECORD).consumes(HttpAttrType.CONTENT_TYPE_JSON.getValue()).produces(HttpAttrType.CONTENT_TYPE_JSON.getValue()).handler(lockHandler::downloadOpenLocklist);
            router.post(ApiConf.UPDATE_USER_PREMISSON).consumes(HttpAttrType.CONTENT_TYPE_JSON.getValue()).produces(HttpAttrType.CONTENT_TYPE_JSON.getValue()).handler(lockHandler::updateNormalDevlock);
            router.post(ApiConf.REQUEST_USER_OPEN_LOCK).consumes(HttpAttrType.CONTENT_TYPE_JSON.getValue()).produces(HttpAttrType.CONTENT_TYPE_JSON.getValue()).handler(lockHandler::adminOpenLock);
            router.post(ApiConf.GET_DEV_LIST).consumes(HttpAttrType.CONTENT_TYPE_JSON.getValue()).produces(HttpAttrType.CONTENT_TYPE_JSON.getValue()).handler(lockHandler::getAdminDevlist);
            router.post(ApiConf.GET_DEV_USER_LIST).consumes(HttpAttrType.CONTENT_TYPE_JSON.getValue()).produces(HttpAttrType.CONTENT_TYPE_JSON.getValue()).handler(lockHandler::getNormalDevlist);
            router.post(ApiConf.EDIT_ADMIN_DEV).consumes(HttpAttrType.CONTENT_TYPE_JSON.getValue()).produces(HttpAttrType.CONTENT_TYPE_JSON.getValue()).handler(lockHandler::editAdminDev);
            router.post(ApiConf.GET_DEV_LONGTITUDE).consumes(HttpAttrType.CONTENT_TYPE_JSON.getValue()).produces(HttpAttrType.CONTENT_TYPE_JSON.getValue()).handler(lockHandler::getAdminDevlocklongtitude);
            router.post(ApiConf.UPDATE_ADMIN_DEV_AUTO_LOCK).consumes(HttpAttrType.CONTENT_TYPE_JSON.getValue()).produces(HttpAttrType.CONTENT_TYPE_JSON.getValue()).handler(lockHandler::updateAdminDevAutolock);
            router.post(ApiConf.UPDATE_DEV_NICKNAME).consumes(HttpAttrType.CONTENT_TYPE_JSON.getValue()).produces(HttpAttrType.CONTENT_TYPE_JSON.getValue()).handler(lockHandler::updateAdminlockNickName);
            router.post(ApiConf.CHECK_DEV).consumes(HttpAttrType.CONTENT_TYPE_JSON.getValue()).produces(HttpAttrType.CONTENT_TYPE_JSON.getValue()).handler(lockHandler::checkAdmindev);
            router.post(ApiConf.UPLOAD_OPEN_LOCK_RECORD).consumes(HttpAttrType.CONTENT_TYPE_JSON.getValue()).produces(HttpAttrType.CONTENT_TYPE_JSON.getValue()).handler(lockHandler::uploadOpenLockList);

            createHttpServer();//创建httpServer
        } else {
            // failed!
            logger.fatal(res.cause().getMessage(), res.cause());
        }
    }


    /**
     * @Description 创建http服务器
     * @author zhang bo
     * @date 17-12-8
     * @version 1.0
     */
    public void createHttpServer() {
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
