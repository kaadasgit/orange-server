package cn.orangeiot.reg.ota;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-03-29
 */
public interface OtaAddr {

    String SELECT_MODEL = "selectModel";//查詢所有產品類型

    String SELECT_DATE_RANGE = "selectDateRange";//查詢日期範圍

    String SELECT_NUM_RANGE = "selectNumRange";//查詢编号范围

    String SUBMIT_OTA_UPGRADE = "submitOTAUpgrade";//提交升级的范围

    String OTA_UPGRADE_PROCESS = "otaUpgradeProcess";//ota升級處理

    String OTA_SELECT_DATA = "otaDeviceData";//ota查詢數據

    String OTA_APPROVATE_RECORD = "otaApprovateRecord";//ota升級審批記錄

}
