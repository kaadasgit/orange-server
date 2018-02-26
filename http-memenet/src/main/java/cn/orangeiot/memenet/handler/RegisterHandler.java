package cn.orangeiot.memenet.handler;

import cn.orangeiot.memenet.client.HttpClient;
import cn.orangeiot.memenet.handler.device.DeviceHandler;
import cn.orangeiot.memenet.handler.user.UserHandler;
import cn.orangeiot.reg.EventbusAddr;
import cn.orangeiot.reg.memenet.MemenetAddr;
import cn.orangeiot.reg.message.MessageAddr;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * @author zhang bo
 * @version 1.0 集群的handler事件注册
 * @Description  MIMI 网第三方的事件注册
 * @date 2017-11-23
 */
public class RegisterHandler implements EventbusAddr{

    private static Logger logger = LogManager.getLogger(RegisterHandler.class);

    private JsonObject config;

    public RegisterHandler(JsonObject config) {
        this.config=config;
    }

    /**
     * @Description 注册事件
     * @author zhang bo
     * @date 17-11-23
     * @version 1.0
     */
    public void consumer(AsyncResult<Vertx> res){
        if (res.succeeded()) {
            Vertx vertx = res.result();


            //注册httpclient
            HttpClient httpClient=new HttpClient();
            httpClient.webClientConf(vertx);


            //用户相关
            UserHandler userHandler=new UserHandler(config,vertx);
            vertx.eventBus().consumer(MemenetAddr.class.getName()+REGISTER_USER,userHandler::onRegisterUser);
            vertx.eventBus().consumer(MemenetAddr.class.getName()+UPDATE_PWD,userHandler::onUpdatePwd);
            vertx.eventBus().consumer(MemenetAddr.class.getName()+REGISTER_USER_BULK,userHandler::onRegisterUserBulk);

            //设备相关
            DeviceHandler deviceHandler=new DeviceHandler(config,vertx);
            vertx.eventBus().consumer(MemenetAddr.class.getName()+BIND_DEVICE_USER,deviceHandler::onBindDeviceByUser);
            vertx.eventBus().consumer(MemenetAddr.class.getName()+RELIEVE_DEVICE_USER,deviceHandler::onRelieveDeviceByUser);
            vertx.eventBus().consumer(MemenetAddr.class.getName()+DEL_DEVICE,deviceHandler::onDelDevice);
            vertx.eventBus().consumer(MemenetAddr.class.getName()+DEL_DEVICE_USER,deviceHandler::onDelGatewayByUser);
        } else {
            // failed!
            logger.fatal(res.cause().getMessage(), res.cause());
        }
    }



}
