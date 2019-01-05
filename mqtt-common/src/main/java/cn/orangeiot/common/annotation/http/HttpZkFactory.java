package cn.orangeiot.common.annotation.http;

import cn.orangeiot.common.annotation.http.impl.HttpManagentMsg;
import cn.orangeiot.common.annotation.http.impl.HttpServerMsg;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;

/**
 * @author : baijun
 * @date : 2019-01-04
 * @description : http 消息处理工厂类
 */
public enum HttpZkFactory {

    Instance;

    /**
     * 处理 Http-Managent 相关参数
     * @param port http服务端口
     * @param path 父节点路径
     * @param aClass 接口常量类
     * @param curatorFramework zk客户端
     */
    public void handleHttpManagentMsg(int port, String path, Class aClass, CuratorFramework curatorFramework,CreateMode mode) {
        HttpManagentMsg httpManagentMsg = new HttpManagentMsg(port,path,aClass,curatorFramework,mode);
        httpManagentMsg.save();
    }

    /**
     * 处理 Http-Server 相关参数
     * @param port http服务端口
     * @param path 父节点路径
     * @param aClass 接口常量类
     * @param curatorFramework zk客户端
     * @param mode 节点类型
     */
    public void handleHttpServerMsg(int port, String path, Class aClass, CuratorFramework curatorFramework, CreateMode mode) {
        HttpServerMsg httpServerMsg = new HttpServerMsg(port,path,aClass,curatorFramework,mode);
        httpServerMsg.save();
    }
}
