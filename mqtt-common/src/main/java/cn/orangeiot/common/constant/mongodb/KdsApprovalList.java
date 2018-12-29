package cn.orangeiot.common.constant.mongodb;

/**
 * @author : baijun
 * @date : 2018-12-27
 * @description : 审批网关绑定列表
 */
public interface KdsApprovalList {

    String COLLECT_NAME = "kdsApprovalList";
    String _ID = "_id"; // 唯一

    String DEVICE_SN = "deviceSN"; // 设备SN
    String UID = "uid"; // 用户标识，外键（kdsUser的_id）
    String DEVICE_NICK_NAME = "deviceNickName"; // 设备昵称
    String USER_NAME = "username"; // 用户名
    String USER_NICK_NAME = "userNickname"; // 用户昵称
    String APPROVAL_UID = "approvaluid"; // 审批用户的主键 （外键kdsUser _id)
    String APPROVAL_NAME = "approvalName"; // 审批账号
    String APPROVAL_NICK_NAME = "approvalNickname"; // 审批者昵称
    String REQUEST_TIME = "requestTime"; // 管理员UID 申请时间
    String TYPE = "type"; // 当前审批状态（1：审批中）
    String APPROVAL_TIME = "approvalTime"; // 审批时间
    String STATUS = "status"; // 状态（1：有效）
    String FAILURE_TIME = "failureTime"; // 失败时间

}
