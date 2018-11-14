package cn.orangeiot.publish.handler.message;

import cn.orangeiot.publish.service.AdminLockService;
import cn.orangeiot.publish.service.ApprovateService;
import cn.orangeiot.publish.service.GatewayDeviceService;
import cn.orangeiot.publish.service.LockService;
import cn.orangeiot.publish.service.impl.*;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.Objects;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-12-22
 */
public class FuncHandler {

    private static Logger logger = LogManager.getLogger(FuncHandler.class);


    private JsonObject jsonObject;

    private Vertx vertx;

    private AdminLockService adminlockService;

    private GatewayDeviceService gatewaydeviceService;

    private ApprovateService approvateService;

    private UserServiceImpl userService;

    private LockService lockService;

    public FuncHandler(Vertx vertx, JsonObject jsonObject) {
        this.vertx = vertx;
        this.jsonObject = jsonObject;
        adminlockService = new AdminLockServiceImpl(vertx, jsonObject);
        gatewaydeviceService = new GatewayDeviceServiceImpl(vertx, jsonObject);
        approvateService = new ApprovateServiceImpl(vertx, jsonObject);
        userService = new UserServiceImpl(vertx, jsonObject);
        lockService = new LockServiceImpl(vertx, jsonObject);
    }


    /**
     * 业务处理
     *
     * @param message
     */
    public void onMessage(Message<JsonObject> message, Handler<AsyncResult<JsonObject>> handler) {
        switch (message.body().getString("func")) {
//            case "createadmindev"://添加设备
//                adminlockService.createAdminDev(message.body(), rs -> {
//                    handler.handle(Future.succeededFuture(rs.result()));
//                });
//                break;
//            case "deletevendordev"://重置解绑
//                adminlockService.deletevendorDev(message.body(), rs -> {
//                    handler.handle(Future.succeededFuture(rs.result()));
//                });
//                break;
//            case "deleteadmindev"://用户主动删除设备
//                adminlockService.deleteAdminDev(message.body(), rs -> {
//                    handler.handle(Future.succeededFuture(rs.result()));
//                });
//                break;
//            case "deletenormaldev"://管理员删除用户
//                adminlockService.deleteNormalDev(message.body(), rs -> {
//                    handler.handle(Future.succeededFuture(rs.result()));
//                });
//                break;
//            case "createNormalDev"://管理员为设备添加普通用户
//                adminlockService.createNormalDev(message.body(), rs -> {
//                    handler.handle(Future.succeededFuture(rs.result()));
//                });
//                break;
//            case "downloadopenlocklist"://获取开锁记录
//                adminlockService.downloadOpenLocklist(message.body(), rs -> {
//                    handler.handle(Future.succeededFuture(rs.result()));
//                });
//                break;
//            case "updateNormalDevlock"://管理员修改普通用户权限
//                adminlockService.updateNormalDevlock(message.body(), rs -> {
//                    handler.handle(Future.succeededFuture(rs.result()));
//                });
//                break;
//            case "adminOpenLock"://用户申请开锁
//                adminlockService.adminOpenLock(message.body(), rs -> {
//                    handler.handle(Future.succeededFuture(rs.result()));
//                });
//                break;
//            case "getAdminDevlist"://获取设备列表
//                adminlockService.getAdminDevlist(message.body(), rs -> {
//                    handler.handle(Future.succeededFuture(rs.result()));
//                });
//                break;
//            case "getNormalDevlist"://设备下的普通用户列表
//                adminlockService.getNormalDevlist(message.body(), rs -> {
//                    handler.handle(Future.succeededFuture(rs.result()));
//                });
//                break;
//            case "editadmindev"://管理员修改锁的位置信息
//                adminlockService.editAdminDev(message.body(), rs -> {
//                    handler.handle(Future.succeededFuture(rs.result()));
//                });
//                break;
//            case "getAdminDevlocklongtitude"://获取设备经纬度等信息
//                adminlockService.getAdminDevlocklongtitude(message.body(), rs -> {
//                    handler.handle(Future.succeededFuture(rs.result()));
//                });
//                break;
//            case "updateAdminDevAutolock"://修改设备是否开启自动解锁功能
//                adminlockService.updateAdminDevAutolock(message.body(), rs -> {
//                    handler.handle(Future.succeededFuture(rs.result()));
//                });
//                break;
//            case "updateAdminlockNickName"://修改设备是否开启自动解锁功能
//                adminlockService.updateAdminlockNickName(message.body(), rs -> {
//                    handler.handle(Future.succeededFuture(rs.result()));
//                });
//                break;
//            case "checkadmindev"://检测是否被绑定
//                adminlockService.checkAdmindev(message.body(), rs -> {
//                    handler.handle(Future.succeededFuture(rs.result()));
//                });
//                break;
//            case "uploadopenlocklist"://上传开门记录
//                adminlockService.uploadOpenLockList(message.body(), rs -> {
//                    handler.handle(Future.succeededFuture(rs.result()));
//                });
//                break;
            case "bindGatewayByUser"://用户绑定网关
                gatewaydeviceService.bindGatewayByUser(message.body(), message.headers().get("qos"), message.headers().get("messageId"), rs -> {
                    handler.handle(Future.succeededFuture(rs.result()));
                });
                break;
            case "approvalBindGateway"://审批普通用户绑定网关
                gatewaydeviceService.approvalBindGateway(message.body(), message.headers().get("qos"), message.headers().get("messageId"), rs -> {
                    handler.handle(Future.succeededFuture(rs.result()));
                });
                break;
            case "gatewayBindList"://获取网关绑定列表
                gatewaydeviceService.getGatewayBindList(message.body(), rs -> {
                    handler.handle(Future.succeededFuture(rs.result()));
                });
                break;
            case "getGatewayaprovalList"://获取审批列表
                gatewaydeviceService.approvalList(message.body(), rs -> {
                    handler.handle(Future.succeededFuture(rs.result()));
                });
                break;
            case "unbindGateway"://取消解绑
                gatewaydeviceService.unbindGateway(message.body(), message.headers().get("qos"), message.headers().get("messageId"), rs -> {
                    handler.handle(Future.succeededFuture(rs.result()));
                });
                break;
            case "delGatewayUser"://删除网关用户
                gatewaydeviceService.delGatewayUser(message.body(), rs -> {
                    handler.handle(Future.succeededFuture(rs.result()));
                });
                break;
            case "getGatewayUserList"://获取网关普通用户集
                gatewaydeviceService.getGatewayUserList(message.body(), rs -> {
                    handler.handle(Future.succeededFuture(rs.result()));
                });
                break;
            case "getDeviceList"://获取网关下的設備列表
                gatewaydeviceService.getDeviceList(message.body(), rs -> {
                    handler.handle(Future.succeededFuture(rs.result()));
                });
                break;
            case "otaApprovateResult"://审批ota升级请求
                approvateService.approvateOTA(message.body(), message.headers().get("qos"), message.headers().get("messageId"), rs -> {
                    handler.handle(rs);
                });
                break;
            case "selectOpenLockRecord"://查询開門记录
                gatewaydeviceService.selectOpenLockRecord(message.body(), rs -> {
                    handler.handle(rs);
                });
                break;
            case "updateDevNickName"://修改设备昵称
                gatewaydeviceService.updateDevNickName(message.body(), rs -> {
                    handler.handle(rs);
                });
                break;
            case "getGatewayDevList"://获取网关下设备
                gatewaydeviceService.getGatewayByDeviceList(message.body(), rs -> {
                    handler.handle(rs);
                });
                break;
            default:
                handler.handle(Future.failedFuture("func resources not find "));
                break;
        }
    }


    /**
     * rpc义务处理
     *
     * @param message
     */
    public void onRpcMessage(Message<JsonObject> message, Handler<AsyncResult<JsonObject>> handler) {
        if (Objects.nonNull(message.body().getValue("func"))) {
            switch (message.body().getString("func")) {
                case "selectGWAdmin"://获取网关管理员
                    userService.selectGWAdmin(message.body());
                    handler.handle(Future.failedFuture("selectGWAdmin process successs"));
                    break;
                case "openLock"://开门
                    if (message.body().getString("clientId").indexOf("gw:") >= 0
                            && Objects.nonNull(message.body().getValue("params"))
                            && message.body().getJsonObject("params").getString("optype").equals("unlock"))
                        lockService.openLock(message.body());
                    handler.handle(Future.succeededFuture());
                    break;
                default:
                    handler.handle(Future.succeededFuture());
                    break;
            }
        } else {
            handler.handle(Future.failedFuture("onRpcMessage func is null"));
        }
    }


    /**
     * rpc 网关响应处理
     *
     * @param message
     */
    public void onRpcGatewayResponseMessage(Message<JsonObject> message, Handler<AsyncResult<JsonObject>> handler) {
        if (Objects.nonNull(message.body().getValue("func"))) {
//            switch (message.body().getString("func")) {
//                case "openLock"://开门
//                    if (message.body().getString("clientId").indexOf("gw:") >= 0
//                            && Objects.nonNull(message.body().getValue("params"))
//                            && message.body().getJsonObject("params").getString("optype").equals("unlock")
//                            && Objects.nonNull(message.body().getValue("returnCode"))
//                            && message.body().getString("returnCode").equals("200"))
//                        lockService.openLock(message.body());
//                    handler.handle(Future.succeededFuture());
//                    break;
//                default:
//                    handler.handle(Future.succeededFuture());
//                    break;
//            }
            handler.handle(Future.succeededFuture());
        } else {
            handler.handle(Future.failedFuture("onRpcMessage func is null"));
        }
    }
}
