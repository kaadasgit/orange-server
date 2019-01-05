package cn.orangeiot.common.annotation.http;

import cn.orangeiot.common.annotation.KdsHttpMessage;

public interface TestApi {
    /**
     * api路径
     */
    @KdsHttpMessage(Method = "get")
    String PRODUCTION_DEVICESN = "/production/deviceSN/:model/:child/:count";//设备SN号生产

    @KdsHttpMessage(Method = "get")
    String PRODUCTION_MODELSN = "/production/modelSN/:model/:child/:count";//模块SN号生产

    @KdsHttpMessage(Method = "post")
    String UPLOAD_MODEL_MAC = "/upload/model/mac";//模块MAC写入

    @KdsHttpMessage(Method = "post")
    String UPLOAD_FILE_MAC="/file/uploadMac";//上传mac文件

    @KdsHttpMessage(Method = "post")
    String UPLOAD_FILE_MAC_RESULT="/file/getMacResult";//上传mac文件的结果

    @KdsHttpMessage(Method = "get")
    String REGISTER_USER_BULK = "/user/bulk/register";//用户米米网批量注册

    @KdsHttpMessage(Method = "post")
    String SELECT_MODEL = "/ota/select/modelAll";//查詢所有產品型號

    @KdsHttpMessage(Method = "post")
    String SELECT_DATE_RANGE = "/ota/select/dateRange";//查詢時間範圍

    @KdsHttpMessage(Method = "post")
    String SELECT_NUM_RANGE = "/ota/select/numRange";//查詢编号范围

    @KdsHttpMessage(Method = "post")
    String SUBMIT_UPGRADE_DATA = "/ota/submit";//提交升級的數據

    @KdsHttpMessage(Method = "post")
    String UPLOAD_DEVICE_TEST_INFO="/file/device/info/test";//上传设备测试信息

    @KdsHttpMessage(Method = "post")
    String UPLOAD_DEVICE_BIND="/upload/device/bind";//上傳設備預綁定 文檔
}
