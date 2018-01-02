package cn.orangeiot.publish.service.impl;

import cn.orangeiot.publish.handler.message.FuncHandler;
import cn.orangeiot.publish.service.BaseService;
import cn.orangeiot.reg.adminlock.AdminlockAddr;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-12-22
 */
public class AdminlockServiceImpl extends BaseService implements AdminlockAddr{

    private static Logger logger = LoggerFactory.getLogger(FuncHandler.class);

    public AdminlockServiceImpl(Vertx vertx, JsonObject jsonObject) {
        super(vertx,jsonObject);
    }

    /**
     * 添加设备
     *
     * @return
     */
    @SuppressWarnings("Duplicates")
    public void createAdminDev(JsonObject jsonObject, Handler<AsyncResult<JsonObject>> handler) {
        send(AdminlockAddr.class.getName() + CREATE_ADMIN_DEV,jsonObject,rs->
                handler.handle(Future.succeededFuture(JsonObject.mapFrom(rs.result())))
        );
    }


    /**
     * @Description 第三方重置设备
     * @author zhang bo
     * @date 17-12-22
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void deletevendorDev(JsonObject jsonObject, Handler<AsyncResult<JsonObject>> handler) {
        send(AdminlockAddr.class.getName() + DELETE_EVEND_DEV,jsonObject,rs->
                handler.handle(Future.succeededFuture(JsonObject.mapFrom(rs.result())))
        );
    }


    /**
     * @Description 用户主动删除设备
     * @author zhang bo
     * @date 17-12-24
     * @version 1.0
     */
    public void deleteAdminDev(JsonObject jsonObject, Handler<AsyncResult<JsonObject>> handler){
        send(AdminlockAddr.class.getName() + DELETE_ADMIN_DEV,jsonObject,rs->
                handler.handle(Future.succeededFuture(JsonObject.mapFrom(rs.result())))
        );
    }

    /**
     * @Description 管理员删除用户
     * @author zhang bo
     * @date 17-12-24
     * @version 1.0
     */
    public void deleteNormalDev(JsonObject jsonObject, Handler<AsyncResult<JsonObject>> handler){
        send(AdminlockAddr.class.getName() + DELETE_NORMAL_DEV,jsonObject,rs->
                handler.handle(Future.succeededFuture(JsonObject.mapFrom(rs.result())))
        );
    }


    /**
     * @Description 管理员为设备添加普通用户
     * @author zhang bo
     * @date 17-12-24
     * @version 1.0
     */
    public void createNormalDev(JsonObject jsonObject, Handler<AsyncResult<JsonObject>> handler){
        send(AdminlockAddr.class.getName() + CREATE_NORMAL_DEV,jsonObject,rs->
                handler.handle(Future.succeededFuture(JsonObject.mapFrom(rs.result())))
        );
    }


    /**
     * @Description 获取开锁记录
     * @author zhang bo
     * @date 17-12-24
     * @version 1.0
     */
    public void downloadOpenLocklist(JsonObject jsonObject, Handler<AsyncResult<JsonObject>> handler){
        send(AdminlockAddr.class.getName() + GET_OPEN_LOCK_RECORD,jsonObject,rs->
                handler.handle(Future.succeededFuture(JsonObject.mapFrom(rs.result())))
        );
    }


    /**
     * @Description 管理员修改普通用户权限
     * @author zhang bo
     * @date 17-12-24
     * @version 1.0
     */
    public void updateNormalDevlock(JsonObject jsonObject, Handler<AsyncResult<JsonObject>> handler) {
        send(AdminlockAddr.class.getName() + UPDATE_USER_PREMISSON, jsonObject, rs->
                handler.handle(Future.succeededFuture(JsonObject.mapFrom(rs.result())))
        );
    }


    /**
     * @Description 用户申请开锁
     * @author zhang bo
     * @date 17-12-24
     * @version 1.0
     */
    public void adminOpenLock(JsonObject jsonObject, Handler<AsyncResult<JsonObject>> handler) {
        send(AdminlockAddr.class.getName() + REQUEST_USER_OPEN_LOCK, jsonObject, rs->
                handler.handle(Future.succeededFuture(JsonObject.mapFrom(rs.result())))
        );
    }


    /**
     * @Description 获取设备列表
     * @author zhang bo
     * @date 17-12-24
     * @version 1.0
     */
    public void getAdminDevlist(JsonObject jsonObject, Handler<AsyncResult<JsonObject>> handler) {
        send(AdminlockAddr.class.getName() + GET_DEV_LIST, jsonObject, rs->
                handler.handle(Future.succeededFuture(JsonObject.mapFrom(rs.result())))
        );
    }


    /**
     * @Description 设备下的普通用户列表
     * @author zhang bo
     * @date 17-12-24
     * @version 1.0
     */
    public void getNormalDevlist(JsonObject jsonObject, Handler<AsyncResult<JsonObject>> handler) {
        send(AdminlockAddr.class.getName() + GET_DEV_USER_LIST, jsonObject, rs->
                handler.handle(Future.succeededFuture(JsonObject.mapFrom(rs.result())))
        );
    }


    /**
     * @Description 管理员修改锁的位置信息
     * @author zhang bo
     * @date 17-12-24
     * @version 1.0
     */
    public void editAdminDev(JsonObject jsonObject, Handler<AsyncResult<JsonObject>> handler) {
        send(AdminlockAddr.class.getName() + EDIT_ADMIN_DEV, jsonObject, rs->
                handler.handle(Future.succeededFuture(JsonObject.mapFrom(rs.result())))
        );
    }


    /**
     * @Description 获取设备经纬度等信息
     * @author zhang bo
     * @date 17-12-24
     * @version 1.0
     */
    public void getAdminDevlocklongtitude(JsonObject jsonObject, Handler<AsyncResult<JsonObject>> handler) {
        send(AdminlockAddr.class.getName() + GET_DEV_LONGTITUDE, jsonObject,rs->
                handler.handle(Future.succeededFuture(JsonObject.mapFrom(rs.result())))
        );
    }


    /**
     * @Description 修改设备是否开启自动解锁功能
     * @author zhang bo
     * @date 17-12-24
     * @version 1.0
     */
    public void updateAdminDevAutolock(JsonObject jsonObject, Handler<AsyncResult<JsonObject>> handler) {
        send(AdminlockAddr.class.getName() + UPDATE_ADMIN_DEV_AUTO_LOCK, jsonObject, rs->
                handler.handle(Future.succeededFuture(JsonObject.mapFrom(rs.result())))
        );
    }

    /**
     * @Description 修改设备昵称
     * @author zhang bo
     * @date 17-12-24
     * @version 1.0
     */
    public void updateAdminlockNickName(JsonObject jsonObject, Handler<AsyncResult<JsonObject>> handler) {
        send(AdminlockAddr.class.getName() + UPDATE_DEV_NICKNAME, jsonObject, rs->
                handler.handle(Future.succeededFuture(JsonObject.mapFrom(rs.result())))
        );
    }


    /**
     * @Description 检测是否被绑定
     * @author zhang bo
     * @date 17-12-24
     * @version 1.0
     */
    public void checkAdmindev(JsonObject jsonObject, Handler<AsyncResult<JsonObject>> handler) {
        send(AdminlockAddr.class.getName() + CHECK_DEV, jsonObject, rs->
                handler.handle(Future.succeededFuture(JsonObject.mapFrom(rs.result())))
        );
    }


    /**
     * @Description 上传开门记录
     * @author zhang bo
     * @date 17-12-24
     * @version 1.0
     */
    public void uploadOpenLockList(JsonObject jsonObject, Handler<AsyncResult<JsonObject>> handler) {
        send(AdminlockAddr.class.getName() + UPLOAD_OPEN_LOCK_RECORD, jsonObject, rs->
            handler.handle(Future.succeededFuture(JsonObject.mapFrom(rs.result())))
        );
    }
}