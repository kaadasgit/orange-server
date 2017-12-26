package cn.orangeiot.publish.handler.message;

import cn.orangeiot.publish.service.impl.AdminlockServiceImpl;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-12-22
 */
public class FuncHandler {

    private static Logger logger = LoggerFactory.getLogger(FuncHandler.class);


    private JsonObject jsonObject;

    private Vertx vertx;

    private AdminlockServiceImpl adminlockService;

    public FuncHandler(Vertx vertx, JsonObject jsonObject) {
        this.vertx=vertx;
        this.jsonObject=jsonObject;
        adminlockService=new AdminlockServiceImpl(vertx,jsonObject);
    }


    /**
     * 业务处理
     * @param message
     */
    public void onMessage(Message<JsonObject> message, Handler<AsyncResult<JsonObject>> handler) {
          switch (message.body().getString("func")){
              case "createadmindev"://添加设备
                      adminlockService.createAdminDev(message.body(),rs->{
                          handler.handle(Future.succeededFuture(rs.result()));
                      });
                      break;
              case "deletevendordev"://重置解绑
                  adminlockService.deletevendorDev(message.body(),rs->{
                      handler.handle(Future.succeededFuture(rs.result()));
                  });
                  break;
              case "deleteadmindev"://用户主动删除设备
                  adminlockService.deleteAdminDev(message.body(),rs->{
                      handler.handle(Future.succeededFuture(rs.result()));
                  });
                  break;
              case "deletenormaldev"://管理员删除用户
                  adminlockService.deleteNormalDev(message.body(),rs->{
                      handler.handle(Future.succeededFuture(rs.result()));
                  });
                  break;
              case "createNormalDev"://管理员为设备添加普通用户
                  adminlockService.createNormalDev(message.body(),rs->{
                      handler.handle(Future.succeededFuture(rs.result()));
                  });
                  break;
              case "downloadopenlocklist"://获取开锁记录
                  adminlockService.downloadOpenLocklist(message.body(),rs->{
                      handler.handle(Future.succeededFuture(rs.result()));
                  });
                  break;
              case "updateNormalDevlock"://管理员修改普通用户权限
                  adminlockService.updateNormalDevlock(message.body(),rs->{
                      handler.handle(Future.succeededFuture(rs.result()));
                  });
                  break;
              case "adminOpenLock"://用户申请开锁
                  adminlockService.adminOpenLock(message.body(),rs->{
                      handler.handle(Future.succeededFuture(rs.result()));
                  });
                  break;
              case "getAdminDevlist"://获取设备列表
                  adminlockService.getAdminDevlist(message.body(),rs->{
                      handler.handle(Future.succeededFuture(rs.result()));
                  });
                  break;
              case "getNormalDevlist"://设备下的普通用户列表
                  adminlockService.getNormalDevlist(message.body(),rs->{
                      handler.handle(Future.succeededFuture(rs.result()));
                  });
                  break;
              case "editadmindev"://管理员修改锁的位置信息
                  adminlockService.editAdminDev(message.body(),rs->{
                      handler.handle(Future.succeededFuture(rs.result()));
                  });
                  break;
              case "getAdminDevlocklongtitude"://获取设备经纬度等信息
                  adminlockService.getAdminDevlocklongtitude(message.body(),rs->{
                      handler.handle(Future.succeededFuture(rs.result()));
                  });
                  break;
              case "updateAdminDevAutolock"://修改设备是否开启自动解锁功能
                  adminlockService.updateAdminDevAutolock(message.body(),rs->{
                      handler.handle(Future.succeededFuture(rs.result()));
                  });
                  break;
              case "updateAdminlockNickName"://修改设备是否开启自动解锁功能
                  adminlockService.updateAdminlockNickName(message.body(),rs->{
                      handler.handle(Future.succeededFuture(rs.result()));
                  });
                  break;
              case "checkadmindev"://检测是否被绑定
                  adminlockService.checkAdmindev(message.body(),rs->{
                      handler.handle(Future.succeededFuture(rs.result()));
                  });
                  break;
              case "uploadopenlocklist"://上传开门记录
                  adminlockService.uploadOpenLockList(message.body(),rs->{
                      handler.handle(Future.succeededFuture(rs.result()));
                  });
                  break;
              default:
                  handler.handle(Future.failedFuture("func resources not find "));
                  break;
          }
    }



}
