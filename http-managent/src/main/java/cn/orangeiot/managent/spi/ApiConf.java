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
    String PRODUCTION_DEVICESN = "/production/deviceSN/:model/:child/:count";//设备SN号生产

    String PRODUCTION_MODELSN = "/production/modelSN/:model/:child/:count";//模块SN号生产

    String UPLOAD_MODEL_MAC = "/upload/model/mac";//模块MAC写入

    String UPLOAD_FILE_MAC="/file/uploadMac";//上传mac文件

    String UPLOAD_FILE_MAC_RESULT="/file/getMacResult";//上传mac文件的结果

    String REGISTER_USER_BULK = "/user/bulk/register";//用户米米网批量注册

    String SELECT_MODEL = "/ota/select/modelAll";//查詢所有產品型號

    String SELECT_DATE_RANGE = "/ota/select/dateRange";//查詢時間範圍

    String SELECT_NUM_RANGE = "/ota/select/numRange";//查詢编号范围

    String SUBMIT_UPGRADE_DATA = "/ota/submit";//提交升級的數據

    String UPLOAD_DEVICE_TEST_INFO="/file/device/info/test";//上传设备测试信息

    String UPLOAD_DEVICE_BIND="/upload/device/bind";//上傳設備預綁定 文檔

}
