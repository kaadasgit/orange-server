package cn.orangeiot.apidao.handler;

import cn.orangeiot.apidao.client.MongoClient;
import cn.orangeiot.apidao.client.RedisClient;
import cn.orangeiot.apidao.handler.dao.admindev.AdminDevDao;
import cn.orangeiot.apidao.handler.dao.device.DeviceDao;
import cn.orangeiot.apidao.handler.dao.file.FileDao;
import cn.orangeiot.apidao.handler.dao.gateway.GatewayDao;
import cn.orangeiot.apidao.handler.dao.job.JobDao;
import cn.orangeiot.apidao.handler.dao.message.MessageDao;
import cn.orangeiot.apidao.handler.dao.ota.OtaDao;
import cn.orangeiot.apidao.handler.dao.register.RegisterDao;
import cn.orangeiot.apidao.handler.dao.topic.TopicDao;
import cn.orangeiot.apidao.handler.dao.user.UserDao;
import cn.orangeiot.apidao.jwt.JwtFactory;
import cn.orangeiot.reg.EventbusAddr;
import cn.orangeiot.reg.adminlock.AdminlockAddr;
import cn.orangeiot.reg.event.EventAddr;
import cn.orangeiot.reg.file.FileAddr;
import cn.orangeiot.reg.gateway.GatewayAddr;
import cn.orangeiot.reg.memenet.MemenetAddr;
import cn.orangeiot.reg.message.MessageAddr;
import cn.orangeiot.reg.ota.OtaAddr;
import cn.orangeiot.reg.user.UserAddr;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTAuth;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author zhang bo
 * @version 1.0 集群的handler事件注册
 * @Description
 * @date 2017-11-23
 */
public class RegisterHandler implements EventbusAddr {

    private static Logger logger = LogManager.getLogger(RegisterHandler.class);

    private JsonObject config;

    private String args;

    public RegisterHandler(JsonObject config, String args) {
        this.config = config;
        this.args = args;
    }

    /**
     * @Description 注册事件
     * @author zhang bo
     * @date 17-11-23
     * @version 1.0
     */
    public void consumer(AsyncResult<Vertx> res) {
        if (res.succeeded()) {
            Vertx vertx = res.result();


            //注册mongoclient
            MongoClient mongoClient = new MongoClient();
            mongoClient.mongoConf(vertx);

            //注册redisclient
            RedisClient redisClient = new RedisClient();
            redisClient.redisConf(vertx);

            //jwt配置
            JwtFactory jwtFactory = new JwtFactory();
            JWTAuth jwtAuth = jwtFactory.JWTConf(vertx, args);

            //topic处理
            TopicDao topicHandler = new TopicDao();
            vertx.eventBus().consumer(config.getString("consumer_saveTopic"), topicHandler::saveTopic);
            vertx.eventBus().consumer(config.getString("consumer_delTopic"), topicHandler::saveTopic);

            //离线消息储存
            MessageDao messageHandler = new MessageDao();
//            vertx.eventBus().consumer(MessageAddr.class.getName()+SAVE_OFFLINE_MSG, messageHandler::onSaveOfflineMsg);
            vertx.eventBus().consumer(MessageAddr.class.getName() + SAVE_CODE, messageHandler::onSaveVerityCode);
            vertx.eventBus().consumer(MessageAddr.class.getName() + GET_CODE_COUNT, messageHandler::onGetCodeCount);

            //连接处理
            UserDao userDao = new UserDao(jwtAuth, vertx);
            vertx.eventBus().consumer(config.getString("consumer_connect_dao"), userDao::getUser);
            vertx.eventBus().consumer(UserAddr.class.getName() + VERIFY_TEL, userDao::telLogin);
            vertx.eventBus().consumer(UserAddr.class.getName() + VERIFY_MAIL, userDao::mailLogin);
            vertx.eventBus().consumer(UserAddr.class.getName() + VERIFY_LOGIN, userDao::verifyLogin);
            vertx.eventBus().consumer(UserAddr.class.getName() + REGISTER_USER_TEL, userDao::registerTel);
            vertx.eventBus().consumer(UserAddr.class.getName() + REGISTER_USER_MAIL, userDao::registerMail);
            vertx.eventBus().consumer(UserAddr.class.getName() + GET_USER_NICKNAME, userDao::getNickname);
            vertx.eventBus().consumer(UserAddr.class.getName() + UPDATE_USER_NICKNAME, userDao::updateNickname);
            vertx.eventBus().consumer(UserAddr.class.getName() + UPDATE_USER_PWD, userDao::updateUserpwd);
            vertx.eventBus().consumer(UserAddr.class.getName() + FORGET_USER_PWD, userDao::forgetUserpwd);
            vertx.eventBus().consumer(UserAddr.class.getName() + SUGGEST_MSG, userDao::suggestMsg);
            vertx.eventBus().consumer(UserAddr.class.getName() + SUGGEST_MSG, userDao::suggestMsg);
            vertx.eventBus().consumer(UserAddr.class.getName() + USER_LOGOUT, userDao::logOut);
            vertx.eventBus().consumer(UserAddr.class.getName() + MEME_USER, userDao::meMeUser);
            vertx.eventBus().consumer(UserAddr.class.getName() + MEME_REGISTER_USER_BULK, userDao::meMeUserBulk);

            //文件处理
            FileDao fileDao = new FileDao();
            vertx.eventBus().consumer(FileAddr.class.getName() + GET_FILE_HEADER, fileDao::onGetHeaderImg);
            vertx.eventBus().consumer(FileAddr.class.getName() + UPLOAD_HEADER_IMG, fileDao::onUploadHeaderImg);

            //job处理
            JobDao jobDao = new JobDao();
            vertx.eventBus().consumer(config.getString("consumer_verifyCodeCron"), jobDao::onMsgVerifyCodeCount);

            //锁相关处理
            AdminDevDao adminDevDao = new AdminDevDao();
            vertx.eventBus().consumer(AdminlockAddr.class.getName() + CREATE_ADMIN_DEV, adminDevDao::createAdminDev);
            vertx.eventBus().consumer(AdminlockAddr.class.getName() + DELETE_EVEND_DEV, adminDevDao::deletevendorDev);
            vertx.eventBus().consumer(AdminlockAddr.class.getName() + DELETE_ADMIN_DEV, adminDevDao::deleteAdminDev);
            vertx.eventBus().consumer(AdminlockAddr.class.getName() + DELETE_NORMAL_DEV, adminDevDao::deleteNormalDev);
            vertx.eventBus().consumer(AdminlockAddr.class.getName() + CREATE_NORMAL_DEV, adminDevDao::createNormalDev);
            vertx.eventBus().consumer(AdminlockAddr.class.getName() + GET_OPEN_LOCK_RECORD, adminDevDao::downloadOpenLocklist);
            vertx.eventBus().consumer(AdminlockAddr.class.getName() + UPDATE_USER_PREMISSON, adminDevDao::updateNormalDevlock);
            vertx.eventBus().consumer(AdminlockAddr.class.getName() + REQUEST_USER_OPEN_LOCK, adminDevDao::adminOpenLock);
            vertx.eventBus().consumer(AdminlockAddr.class.getName() + GET_DEV_LIST, adminDevDao::getAdminDevlist);
            vertx.eventBus().consumer(AdminlockAddr.class.getName() + GET_DEV_USER_LIST, adminDevDao::getNormalDevlist);
            vertx.eventBus().consumer(AdminlockAddr.class.getName() + EDIT_ADMIN_DEV, adminDevDao::editAdminDev);
            vertx.eventBus().consumer(AdminlockAddr.class.getName() + GET_DEV_LONGTITUDE, adminDevDao::getAdminDevlocklongtitude);
            vertx.eventBus().consumer(AdminlockAddr.class.getName() + UPDATE_ADMIN_DEV_AUTO_LOCK, adminDevDao::updateAdminDevAutolock);
            vertx.eventBus().consumer(AdminlockAddr.class.getName() + UPDATE_DEV_NICKNAME, adminDevDao::updateAdminlockNickName);
            vertx.eventBus().consumer(AdminlockAddr.class.getName() + CHECK_DEV, adminDevDao::checkAdmindev);
            vertx.eventBus().consumer(AdminlockAddr.class.getName() + UPLOAD_OPEN_LOCK_RECORD, adminDevDao::uploadOpenLockList);

            //生产相关
            DeviceDao deviceDao = new DeviceDao();
            vertx.eventBus().consumer(MemenetAddr.class.getName() + PRODUCTION_DEVICESN, deviceDao::productionDeviceSN);
            vertx.eventBus().consumer(AdminlockAddr.class.getName() + MODEL_PRODUCT, deviceDao::productionModelSN);
            vertx.eventBus().consumer(AdminlockAddr.class.getName() + MODEL_MAC_IN, deviceDao::modelMacIn);
            vertx.eventBus().consumer(AdminlockAddr.class.getName() + GET_MODEL_PASSWORD, deviceDao::getPwdByMac);


            //网关相关
            GatewayDao gatewayDao = new GatewayDao();
            vertx.eventBus().consumer(GatewayAddr.class.getName() + BIND_GATEWAY_USER, gatewayDao::onbindGatewayByUser);
            vertx.eventBus().consumer(MessageAddr.class.getName() + GET_GATEWAY_ADMIN, gatewayDao::onGetGatewayAdmin);
            vertx.eventBus().consumer(GatewayAddr.class.getName() + APPROVAL_GATEWAY_BIND, gatewayDao::onApprovalBindGateway);
            vertx.eventBus().consumer(GatewayAddr.class.getName() + GET_GATEWAY_BIND_LIST, gatewayDao::onGetGatewayBindList);
            vertx.eventBus().consumer(GatewayAddr.class.getName() + GET_GATEWAY_APPROVAL_LIST, gatewayDao::onApprovalList);
            vertx.eventBus().consumer(GatewayAddr.class.getName() + GET_USERINFO, gatewayDao::onGetUserInfo);
            vertx.eventBus().consumer(GatewayAddr.class.getName() + UPDATE_GATEWAY_DOMAIN, gatewayDao::onupdateGWDomain);
            vertx.eventBus().consumer(GatewayAddr.class.getName() + UNBIND_GATEWAY, gatewayDao::onUnbindGateway);
            vertx.eventBus().consumer(GatewayAddr.class.getName() + GET_GATEWWAY_USERID_LIST, gatewayDao::onGetUserIdList);
            vertx.eventBus().consumer(GatewayAddr.class.getName() + DEL_GW_USER, gatewayDao::onDelGatewayUser);
            vertx.eventBus().consumer(GatewayAddr.class.getName() + GET_GW_USER_LIST, gatewayDao::onGetGatewayUserList);
            vertx.eventBus().consumer(EventAddr.class.getName() + GET_GATEWAY_ADMIN_UID, gatewayDao::onGetGatewayAdminByuid);
            vertx.eventBus().consumer(GatewayAddr.class.getName() + DEVICE_ONLINE, gatewayDao::deviceOnline);
            vertx.eventBus().consumer(GatewayAddr.class.getName() + DEVICE_OFFLINE, gatewayDao::deviceOffline);
            vertx.eventBus().consumer(GatewayAddr.class.getName() + GET_DEVICE_List, gatewayDao::getDeviceList);


            //stream 相關
            RegisterDao registerDao = new RegisterDao();
            vertx.eventBus().consumer(UserAddr.class.getName() + SAVE_REGISTER_USER, registerDao::saveRegisterInfo);
            vertx.eventBus().consumer(UserAddr.class.getName() + GET_REGISTER_USER, registerDao::getRegisterInfo);
            vertx.eventBus().consumer(UserAddr.class.getName() + DEL_REGISTER_USER, registerDao::delRegisterInfo);
            vertx.eventBus().consumer(UserAddr.class.getName() + SAVE_CALL_ID, registerDao::saveCallIdAddr);
            vertx.eventBus().consumer(UserAddr.class.getName() + GET_CALL_ID, registerDao::getCallIdAddr);


            //ota 升級相關
            OtaDao otaDao=new OtaDao();
            vertx.eventBus().consumer(OtaAddr.class.getName() + SELECT_MODEL, otaDao::selectModelType);
            vertx.eventBus().consumer(OtaAddr.class.getName() + SELECT_DATE_RANGE, otaDao::selectDateRange);
            vertx.eventBus().consumer(OtaAddr.class.getName() + SELECT_NUM_RANGE, otaDao::selectNumRange);
        } else {
            // failed!
            logger.error(res.cause().getMessage(), res.cause());
        }
    }


}
