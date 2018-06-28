package cn.orangeiot.reg.gateway;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-01-04
 */
public interface GatewayAddr {

    String BIND_GATEWAY_USER="bindGatewayByUser";//用户绑定网关

    String APPROVAL_GATEWAY_BIND="approvalGatewaybind";//审批用户绑定网关

    String GET_GATEWAY_BIND_LIST="gatewayBindList";//获取用户网关绑定的列表

    String GET_GATEWAY_APPROVAL_LIST="getGatewayaprovalList";//获取审批列表

    String GET_USERINFO="getUserInfo";//获取用户信息

    String UPDATE_GATEWAY_DOMAIN="updateGWDomain";//修改设备域

    String UNBIND_GATEWAY="unbindGW";//解绑网关

    String GET_GATEWWAY_USERID_LIST="getGWUseridList";//绑定网关打用户集

    String DEL_GW_USER="delGWUser";//管理员删除用户

    String GET_GW_USER_LIST="getGWUserList";//获取网关普通用户

    String DEVICE_ONLINE="deviceOnline";//設備上線

    String DEVICE_OFFLINE="deviceOffline";//設備上線

    String DEVICE_DELETE="deviceDelete";//設備刪除

    String GET_DEVICE_List="getDeviceList";//獲取設備列表

    String GET_GW_DEVICE_List="getGWDeviceList";//獲取网关設備列表
}
