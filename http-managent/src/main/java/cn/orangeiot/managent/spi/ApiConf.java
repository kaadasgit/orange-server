package cn.orangeiot.managent.spi;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-12-08
 */
public interface ApiConf {

    /**
     * api路径
     */
    String PRODUCTION_DEVICESN = "/production/deviceSN/:count";//设备SN号生产

    String PRODUCTION_MODELSN = "/production/modelSN/:count/:factory/:model";//模块SN号生产

    String UPLOAD_MODEL_MAC = "/upload/model/mac";//模块MAC写入

    String REGISTER_USER_BULK = "/user/bulk/register";//用户米米网批量注册

}
