package cn.orangeiot.common.constant.mongodb;
/**
 * @author : baijun
 * @date : 2018-12-27
 * @Description : 用户头像数据，和kdsUser表外键关联(uid:用户ID)
 */
public interface KdsUserHead {

    String COLLECT_NAME = "kdsUserHead";
    String _ID = "_id"; // 主键 唯一性 自动生成

    String _CLASS = "_class";
    String NAME = "name"; // 名称
    String CONTENT_TYPE = "contentType"; // 文件类型
    String SIZE = "size"; // 文件大小（单位：byte）
    String UPLOAD_DATE = "uploadDate"; // 上传时间
    String CONTENT = "content"; // 头像数据
    String UID = "uid"; // 用户id (kdsUser主键)

}
