package cn.orangeiot.common.constant.mongodb;
/**
 * @author : baijun
 * @date : 2018-12-27
 */
public interface KdsbindGWHistoryRecord {

    String COLLECT_NAME = "kdsbindGWHistoryRecord";
    String _ID = "_id";

    String DEVICE_SN = "deviceSN";
    String DEVICE_LIST = "deviceList";

    // 设备列表第二级 key
    String _DL_EVENT_STR = "event_str";
    String _DL_MACADDR = "macaddr";
    String _DL_SW = "SW";
    String _DL_DEVICE_TYPE = "device_type";
    String _DL_NWADDR = "nwaddr";
    String _DL_DEVICE_ID = "deviceId";
    String _DL_TIME = "time";
    String _DL_IPADDR = "ipaddr";

    String UID = "uid";
    String BIND_TIME = "bindTime";
    String UNBIND_TIME = "unbindTime";

}
