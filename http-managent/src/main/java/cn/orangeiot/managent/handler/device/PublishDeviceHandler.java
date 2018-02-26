package cn.orangeiot.managent.handler.device;

import cn.orangeiot.common.genera.ErrorType;
import cn.orangeiot.common.genera.Result;
import cn.orangeiot.common.options.SendOptions;
import cn.orangeiot.common.utils.DataType;
import cn.orangeiot.common.utils.UUIDUtils;
import cn.orangeiot.managent.utils.ExcelUtil;
import cn.orangeiot.managent.verify.VerifyParamsUtil;
import cn.orangeiot.reg.EventbusAddr;
import cn.orangeiot.reg.adminlock.AdminlockAddr;
import cn.orangeiot.reg.memenet.MemenetAddr;
import io.vertx.core.AsyncResult;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.impl.AsyncFileImpl;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.Pump;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.omg.CORBA.INTERNAL;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-01-02
 */
public class PublishDeviceHandler implements EventbusAddr {

    private static Logger logger = LogManager.getLogger(PublishDeviceHandler.class);


    private EventBus eventBus;

    private JsonObject config;

    public PublishDeviceHandler(EventBus eventBus, JsonObject config) {
        this.eventBus = eventBus;
        this.config = config;
    }

    /**
     * @Description 生产设备SN号导入
     * @author zhang bo
     * @date 18-1-2
     * @version 1.0
     */
    public void productionDeviceSN(RoutingContext routingContext) {
        logger.info("==PublishDeviceHandler=productionDeviceSN==params->" + routingContext.getBodyAsString());
        if (Objects.nonNull(routingContext.request().getParam("count"))) {
            //根据数量生产deviceSN
            List<String> list = new ArrayList<>(Integer.parseInt(routingContext.request().getParam("count")));//设备SN集合
            for (int i = 0; i < Integer.parseInt(routingContext.request().getParam("count")); i++) {
                list.add(UUIDUtils.getUUID());
            }
            //数据入库
            eventBus.send(MemenetAddr.class.getName() + PRODUCTION_DEVICESN, new JsonObject().put("deviceSNList", new JsonArray(list))
                    , rs -> {
                        if (rs.failed()) {
                            rs.cause().printStackTrace();
                            routingContext.fail(501);
                        } else {
                            if (Objects.nonNull(rs.result())) {
                                ByteArrayOutputStream os = new ByteArrayOutputStream();
                                ExcelUtil.exportExcelX("注册的SN设备号", new HashMap<String, String>() {{
                                    put("num", "序号");
                                    put("devuuid", "设备devuuid号");
                                }}, new JsonArray(list), null, 0, os);
                                byte[] content = os.toByteArray();
                                routingContext.response().setChunked(true).putHeader("Content-type", "application/octet-stream")
                                        .putHeader("Content-Disposition", " attachment; filename=production_deviceSN.xlsx")
                                        .write(Buffer.buffer().appendBytes(content)).end();//分块编码

                            } else {//失败
                                routingContext.response().end(JsonObject.mapFrom(new Result<String>()
                                        .setErrorMessage(ErrorType.PRODUCTION_DEVICESN_FAIL.getKey(), ErrorType.PRODUCTION_DEVICESN_FAIL.getValue())).toString());
                            }
                        }

                    });
        } else {
            routingContext.fail(401);
        }
    }


    /**
     * @Description 生产BLE SN设备号 password1
     * @author zhang bo
     * @date 18-1-23
     * @version 1.0
     */
    public void productionBLESN(RoutingContext routingContext) {
        logger.info("==PublishDeviceHandler=productionBLESN==params->" + routingContext.getBodyAsString());
        //校验数据
        if (Objects.nonNull(routingContext.request().getParam("count"))
                && Objects.nonNull(routingContext.request().getParam("factory"))
                && Objects.nonNull(routingContext.request().getParam("model"))) {
            //数据入库
            try {
                eventBus.send(AdminlockAddr.class.getName() + MODEL_PRODUCT, new JsonObject()
                                .put("count", Integer.parseInt(routingContext.request().getParam("count")))
                                .put("factory", routingContext.request().getParam("factory"))
                                .put("model", routingContext.request().getParam("model"))
                        , (AsyncResult<Message<JsonArray>> as) -> {
                            if (as.failed()) {
                                routingContext.fail(501);
                            } else {
                                if (Objects.nonNull(as.result().body())) {
                                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                                    ExcelUtil.exportModelExcel("模块SN设备号", new HashMap<String, String>() {{
                                        put("num", "序号");
                                        put("SN", "模块SN号");
                                        put("password1", "password1");
                                    }}, as.result().body(), null, 0, os);
                                    byte[] content = os.toByteArray();
                                    routingContext.response().setChunked(true).putHeader("Content-type", "application/octet-stream")
                                            .putHeader("Content-Disposition", " attachment; filename=production_ModelSN.xlsx")
                                            .write(Buffer.buffer().appendBytes(content)).end();//分块编码
                                }
                            }
                        });
            } catch (Exception e) {
                e.getCause().printStackTrace();
                routingContext.fail(401);
            }
        } else {
            routingContext.fail(401);
        }

    }


    /**
     * @Description 上传mac地址
     * @author zhang bo
     * @date 18-1-26
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void uploadMacAddr(RoutingContext routingContext) {
        logger.info("==PublishDeviceHandler=productionDeviceSN==params->" + routingContext.getBodyAsString());
        VerifyParamsUtil.verifyParams(routingContext, new JsonObject().put("SN", DataType.STRING)
                .put("password1", DataType.STRING).put("mac", DataType.STRING), rs -> {
            if (rs.failed()) {
                routingContext.fail(401);
            } else {
                eventBus.send(AdminlockAddr.class.getName() + MODEL_MAC_IN, rs.result()
                        , SendOptions.getInstance(), as -> {
                            if (rs.failed()) {
                                routingContext.fail(501);
                            } else {
                                if (Objects.nonNull(as.result())) {
                                    routingContext.response().end(JsonObject.mapFrom(new Result<>()).toString());
                                } else {
                                    if (!as.result().headers().isEmpty())
                                        routingContext.response().end(JsonObject.mapFrom(
                                                new Result<>().setErrorMessage(Integer.parseInt(as.result().headers().get("code")), as.result().headers().get("msg"))).toString());
                                    else
                                        routingContext.fail(501);
                                }
                            }
                        });
            }
        });
    }

}
