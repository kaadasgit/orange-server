package cn.orangeiot.apidao.handler;

import cn.orangeiot.apidao.client.MongoClient;
import cn.orangeiot.apidao.client.RedisClient;
import cn.orangeiot.apidao.client.StorageClient;
import cn.orangeiot.apidao.handler.dao.admindev.AdminDevDao;
import cn.orangeiot.apidao.handler.dao.device.DeviceDao;
import cn.orangeiot.apidao.handler.dao.file.FileDao;
import cn.orangeiot.apidao.handler.dao.gateway.GatewayDao;
import cn.orangeiot.apidao.handler.dao.job.JobDao;
import cn.orangeiot.apidao.handler.dao.message.MessageDao;
import cn.orangeiot.apidao.handler.dao.ota.OtaDao;
import cn.orangeiot.apidao.handler.dao.rateLimit.RateLimitDao;
import cn.orangeiot.apidao.handler.dao.register.RegisterDao;
import cn.orangeiot.apidao.handler.dao.storage.StorageDao;
import cn.orangeiot.apidao.handler.dao.test.TestProcessDao;
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
import cn.orangeiot.reg.rateLimit.RateLimitAddr;
import cn.orangeiot.reg.storage.StorageAddr;
import cn.orangeiot.reg.testservice.TestProcessAddr;
import cn.orangeiot.reg.user.UserAddr;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTAuth;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Objects;

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

            //注冊storage
//            StorageClient storageClient = new StorageClient();
//            storageClient.loadConf(System.getProperty("STORAGEPATH"));

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
            UserAboutEvent(jwtAuth, vertx);

            //文件处理
            FileDao fileDao = new FileDao();
            vertx.eventBus().consumer(FileAddr.class.getName() + GET_FILE_HEADER, fileDao::onGetHeaderImg);
            vertx.eventBus().consumer(FileAddr.class.getName() + UPLOAD_HEADER_IMG, fileDao::onUploadHeaderImg);

            //job处理
            JobDao jobDao = new JobDao();
            vertx.eventBus().consumer(config.getString("consumer_verifyCodeCron"), jobDao::onMsgVerifyCodeCount);

            //锁相关处理
            lockAboutEvent(vertx);

            //生产相关
            productAboutEvent(vertx);

            //网关相关
            gatewayAboutEvent(vertx);

            //stream 相關
            streamAboutEvent(vertx);

            //ota 升級相關
            oTaAboutEvent(vertx);

            //storage
            storageAboutEvent(vertx);

            //限流相关
            rateLimitAboutEvent(vertx);

            //測試抽查相關
            testProcess(vertx);
        } else {
            // failed!
            logger.fatal(res.cause().getMessage(), res.cause());
            System.exit(1);
        }
    }


    /**
     * @Description 測試處理相關
     * @author zhang bo
     * @date 18-12-25
     * @version 1.0
     */
    public void testProcess(Vertx vertx){
        TestProcessDao testProcessDao=new TestProcessDao(vertx);
        vertx.eventBus().consumer(TestProcessAddr.class.getName() + TEST_UIBIND_GATEWAY, testProcessDao::testUnBindGateway);
    }


    /**
     * @Description 鎖相關事件
     * @author zhang bo
     * @date 18-7-11
     * @version 1.0
     */
    public void lockAboutEvent(Vertx vertx) {
        AdminDevDao adminDevDao = new AdminDevDao(vertx);
        vertx.eventBus().consumer(AdminlockAddr.class.getName() + CREATE_ADMIN_DEV, adminDevDao::createAdminDev);
        vertx.eventBus().consumer(AdminlockAddr.class.getName() + DELETE_EVEND_DEV, adminDevDao::deletevendorDev);
        vertx.eventBus().consumer(AdminlockAddr.class.getName() + DELETE_ADMIN_DEV, adminDevDao::deleteAdminDev);
        vertx.eventBus().consumer(AdminlockAddr.class.getName() + DELETE_NORMAL_DEV, adminDevDao::deleteNormalDev);
        vertx.eventBus().consumer(AdminlockAddr.class.getName() + CREATE_NORMAL_DEV, adminDevDao::createNormalDev);
        vertx.eventBus().consumer(AdminlockAddr.class.getName() + GET_OPEN_LOCK_RECORD, adminDevDao::downloadOpenLocklist);
        vertx.eventBus().consumer(AdminlockAddr.class.getName() + UPDATE_USER_PREMISSON, adminDevDao::updateNormalDevlock);
        vertx.eventBus().consumer(AdminlockAddr.class.getName() + REQUEST_USER_OPEN_LOCK, adminDevDao::adminOpenLock);
        vertx.eventBus().consumer(AdminlockAddr.class.getName() + LOCK_AUTH, adminDevDao::openLockAuth);
        vertx.eventBus().consumer(AdminlockAddr.class.getName() + GET_DEV_LIST, adminDevDao::getAdminDevlist);
        vertx.eventBus().consumer(AdminlockAddr.class.getName() + GET_DEV_USER_LIST, adminDevDao::getNormalDevlist);
        vertx.eventBus().consumer(AdminlockAddr.class.getName() + EDIT_ADMIN_DEV, adminDevDao::editAdminDev);
        vertx.eventBus().consumer(AdminlockAddr.class.getName() + GET_DEV_LONGTITUDE, adminDevDao::getAdminDevlocklongtitude);
        vertx.eventBus().consumer(AdminlockAddr.class.getName() + UPDATE_ADMIN_DEV_AUTO_LOCK, adminDevDao::updateAdminDevAutolock);
        vertx.eventBus().consumer(AdminlockAddr.class.getName() + UPDATE_DEV_NICKNAME, adminDevDao::updateAdminlockNickName);
        vertx.eventBus().consumer(AdminlockAddr.class.getName() + CHECK_DEV, adminDevDao::checkAdmindev);
        vertx.eventBus().consumer(AdminlockAddr.class.getName() + UPLOAD_OPEN_LOCK_RECORD, adminDevDao::uploadOpenLockList);
        vertx.eventBus().consumer(AdminlockAddr.class.getName() + UPDATE_LOCK_INFO, adminDevDao::updateLockInfo);
        vertx.eventBus().consumer(AdminlockAddr.class.getName() + OPEN_LOCK_NO_AUTH_SUCCESS, adminDevDao::openLockNoAuth);
        vertx.eventBus().consumer(AdminlockAddr.class.getName() + UPDATE_LOCK_NUM_INFO, adminDevDao::updateLockNumInfo);
        vertx.eventBus().consumer(AdminlockAddr.class.getName() + UPDATE_BULK_LOCK_NUM_INFO, adminDevDao::updateBulkLockNumInfo);
        vertx.eventBus().consumer(AdminlockAddr.class.getName() + GET_LOCK_NUM_INFO, adminDevDao::getLockNumInfo);
        vertx.eventBus().consumer(AdminlockAddr.class.getName() + SELECT_OPNELOCK_RECORD, adminDevDao::selectOpenLockRecord);
        vertx.eventBus().consumer(AdminlockAddr.class.getName() + OPEN_LOCK_BY_GATEWAY, adminDevDao::openLockByGateway);
    }


    /**
     * @Description 网关相关事件
     * @author zhang bo
     * @date 18-7-11
     * @version 1.0
     */
    public void gatewayAboutEvent(Vertx vertx) {
        GatewayDao gatewayDao = new GatewayDao(vertx);
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
        vertx.eventBus().consumer(EventAddr.class.getName() + GET_GATEWAY_ADMIN_ALL, gatewayDao::onGetGatewayUserAll);
        vertx.eventBus().consumer(GatewayAddr.class.getName() + DEVICE_ONLINE, gatewayDao::deviceOnline);
        vertx.eventBus().consumer(GatewayAddr.class.getName() + DEVICE_OFFLINE, gatewayDao::deviceOffline);
        vertx.eventBus().consumer(GatewayAddr.class.getName() + DEVICE_DELETE, gatewayDao::deviceDelete);
        vertx.eventBus().consumer(GatewayAddr.class.getName() + GET_DEVICE_List, gatewayDao::getDeviceList);
        vertx.eventBus().consumer(GatewayAddr.class.getName() + GET_GW_DEVICE_List, gatewayDao::getDeviceList);
        vertx.eventBus().consumer(GatewayAddr.class.getName() + EVENT_OPEN_LOCK, gatewayDao::EventOpenLock);
        vertx.eventBus().consumer(GatewayAddr.class.getName() + SELECT_OPEN_LOCK_RECORD, gatewayDao::selectOpenLock);
        vertx.eventBus().consumer(GatewayAddr.class.getName() + RESET_DEVICE, gatewayDao::resetDevice);
        vertx.eventBus().consumer(GatewayAddr.class.getName() + UPDATE_GATE_DEV_NICKNAME, gatewayDao::updateDevNickName);
        vertx.eventBus().consumer(GatewayAddr.class.getName() + GET_GATE_DEV_LIST, gatewayDao::getGatewayDevList);
        vertx.eventBus().consumer(GatewayAddr.class.getName() + GET_GATWWAY_USERALL, gatewayDao::getGatewayUserList);
        vertx.eventBus().consumer(GatewayAddr.class.getName() + GET_USER_GATEWAYLIST, gatewayDao::getUserWithGatewayList);
    }


    /**
     * @Description 用戶處理相關
     * @author zhang bo
     * @date 18-7-11
     * @version 1.0
     */
    public void UserAboutEvent(JWTAuth jwtAuth, Vertx vertx) {
        UserDao userDao = new UserDao(jwtAuth, vertx,config);
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
        vertx.eventBus().consumer(UserAddr.class.getName() + USER_LOGOUT, userDao::logOut);
        vertx.eventBus().consumer(UserAddr.class.getName() + MEME_USER, userDao::meMeUser);
        vertx.eventBus().consumer(UserAddr.class.getName() + MEME_REGISTER_USER_BULK, userDao::meMeUserBulk);
        vertx.eventBus().consumer(UserAddr.class.getName() + GET_GW_ADMIN, userDao::selectGWAdmin);
        vertx.eventBus().consumer(UserAddr.class.getName() + UPLOAD_JPUSHID, userDao::uploadPushId);
        vertx.eventBus().consumer(MessageAddr.class.getName() + GET_PUSHID, userDao::getPushId);
        vertx.eventBus().consumer(MessageAddr.class.getName() + BRANCH_SEND_TIMES, userDao::branchSendRecord);
    }


    /**
     * @Description 生产相关的事件
     * @author zhang bo
     * @date 18-7-11
     * @version 1.0
     */
    public void productAboutEvent(Vertx vertx) {
        DeviceDao deviceDao = new DeviceDao(vertx);
        vertx.eventBus().consumer(MemenetAddr.class.getName() + PRODUCTION_DEVICESN, deviceDao::productionDeviceSN);
        vertx.eventBus().consumer(AdminlockAddr.class.getName() + MODEL_PRODUCT, deviceDao::productionModelSN);
        vertx.eventBus().consumer(AdminlockAddr.class.getName() + MODEL_MAC_IN, deviceDao::modelMacIn);
        vertx.eventBus().consumer(AdminlockAddr.class.getName() + GET_MODEL_PASSWORD, deviceDao::getPwdByMac);
        vertx.eventBus().consumer(AdminlockAddr.class.getName() + MODEL_MANY_MAC_IN, deviceDao::modelManyMacIn);
        vertx.eventBus().consumer(AdminlockAddr.class.getName() + DEVICE_TEST_INFO_IN, deviceDao::deviceTestInfoIn);
        vertx.eventBus().consumer(AdminlockAddr.class.getName() + GET_WRITE_MAC_RESULT, deviceDao::getWriteMacResult);
        vertx.eventBus().consumer(AdminlockAddr.class.getName() + UPDATE_PRE_BIND_DEVICE, deviceDao::preBindDevice);
        vertx.eventBus().consumer(AdminlockAddr.class.getName() + PRODUCTION_TEST_USER, deviceDao::productionTestUser);
    }


    /**
     * @Description sip和流相关事件
     * @author zhang bo
     * @date 18-7-11
     * @version 1.0
     */
    public void streamAboutEvent(Vertx vertx){
        RegisterDao registerDao = new RegisterDao();
        vertx.eventBus().consumer(UserAddr.class.getName() + SAVE_REGISTER_USER, registerDao::saveRegisterInfo);
        vertx.eventBus().consumer(UserAddr.class.getName() + HEARTBEAT_REGISTER_USER, registerDao::heartbeatRegisterInfo);
        vertx.eventBus().consumer(UserAddr.class.getName() + GET_REGISTER_USER, registerDao::getRegisterInfo);
        vertx.eventBus().consumer(UserAddr.class.getName() + DEL_REGISTER_USER, registerDao::delRegisterInfo);
        vertx.eventBus().consumer(UserAddr.class.getName() + SAVE_CALL_ID, registerDao::saveCallIdAddr);
        vertx.eventBus().consumer(UserAddr.class.getName() + GET_CALL_ID, registerDao::getCallIdAddr);
        vertx.eventBus().consumer(UserAddr.class.getName() + SAVE_SESSION_BRANCH, registerDao::saveSessionBranch);
        vertx.eventBus().consumer(UserAddr.class.getName() + GET_SESSION_BRANCH, registerDao::getSessionBranch);
        vertx.eventBus().consumer(UserAddr.class.getName() + REMOVE_SESSION_BRANCH, registerDao::removeSessionBranch);
    }


    /**
     * @Description ota 相关事件
     * @author zhang bo
     * @date 18-7-11
     * @version 1.0
     */
    public void oTaAboutEvent(Vertx vertx){
        OtaDao otaDao = new OtaDao();
        vertx.eventBus().consumer(OtaAddr.class.getName() + SELECT_MODEL, otaDao::selectModelType);
        vertx.eventBus().consumer(OtaAddr.class.getName() + SELECT_DATE_RANGE, otaDao::selectDateRange);
        vertx.eventBus().consumer(OtaAddr.class.getName() + SELECT_NUM_RANGE, otaDao::selectNumRange);
        vertx.eventBus().consumer(OtaAddr.class.getName() + SUBMIT_OTA_UPGRADE, otaDao::submitOTAUpgrade);
        vertx.eventBus().consumer(OtaAddr.class.getName() + OTA_SELECT_DATA, otaDao::getUpgradeDevice);
        vertx.eventBus().consumer(OtaAddr.class.getName() + OTA_APPROVATE_RECORD, otaDao::otaApprovateRecord);
    }


    /**
     * @Description 存儲相關
     * @author zhang bo
     * @date 18-7-11
     * @version 1.0
     */
    public void storageAboutEvent(Vertx vertx){
        StorageDao storageDao = new StorageDao(vertx);
        vertx.eventBus().consumer(StorageAddr.class.getName() + PUT_STORAGE_DATA, storageDao::putStorageData);
        vertx.eventBus().consumer(StorageAddr.class.getName() + DEL_STORAGE_DATA, storageDao::delStorageData);
        vertx.eventBus().consumer(StorageAddr.class.getName() + GET_STORAGE_DATA, storageDao::getStorageData);
        vertx.eventBus().consumer(StorageAddr.class.getName() + DELALL_STORAGE_DATA, storageDao::delAllStorageData);
    }


    /**
     * @Description 限流相關事件
     * @author zhang bo
     * @date 18-8-27
     * @version 1.0
     */
    public void rateLimitAboutEvent(Vertx vertx){
        RateLimitDao rateLimitDao=new RateLimitDao();
        vertx.eventBus().consumer(RateLimitAddr.class.getName() + USER_RUST_REQUEST_ADD, rateLimitDao::rustRequestAdd);
    }
}
