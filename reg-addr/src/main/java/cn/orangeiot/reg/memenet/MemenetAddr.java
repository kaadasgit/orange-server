package cn.orangeiot.reg.memenet;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-01-02
 */
public interface MemenetAddr{

    String REGISTER_USER="registerUser";//MIMI用户注册

    String REGISTER_USER_CALLBACK="registerUserCall";//MIMI用户注册回調

    String UPDATE_PWD="updatePwd";//MIMI用户密码修改

    String BIND_DEVICE_USER="bindDeviceByUser";//MIMI用户绑定设备

    String RELIEVE_DEVICE_USER="relieveDeviceByUser";//MIMI用户解除设备绑定

    String DEL_DEVICE_USER="delDeviceByUser";//管理员删除普通用户MIMI解除设备绑定

    String DEL_DEVICE="delDevice";//MIMI终端设备删除

    String PRODUCTION_DEVICESN="productionDeviceSN";//生产设备

    String REGISTER_USER_BULK="registerUserBulk";//MIMI用户批量注册
}
