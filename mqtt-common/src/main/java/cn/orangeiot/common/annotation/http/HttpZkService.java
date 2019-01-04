package cn.orangeiot.common.annotation.http;

/**
 * @author : baijun
 * @date : 2019-01-04
 * @description : Http 消息封装的上层接口（定义处理信息的接口）
 */
public interface HttpZkService {

    /**
     * 构造 urls(url 和 method)
     */
    void createUrls();

    /**
     * 构造 host
     */
    void createHost();

    /**
     * 构造 消息
     */
    String create();

    /**
     * 保存到zookeeper
     */
    void save();

    /**
     * 判断节点路径是否已经存在
     * @return
     */
    boolean exists();
}
