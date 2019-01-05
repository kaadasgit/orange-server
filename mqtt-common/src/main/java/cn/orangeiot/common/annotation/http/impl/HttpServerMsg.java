package cn.orangeiot.common.annotation.http.impl;

import cn.orangeiot.common.annotation.http.AbstractHttpZkService;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;

/**
 * @author : baijun
 * @date : 2019-01-04
 * @description : HttpServer 服务器消息
 */
public class HttpServerMsg extends AbstractHttpZkService {

    public HttpServerMsg(int port, String path, Class aClass, CuratorFramework curatorFramework, CreateMode mode) {
//        this.host = host;
        this.port = port;
        this.path = path;
        this.aClass = aClass;
        this.curatorFramework = curatorFramework;
        this.createMode = mode;

        createHost();
        createUrls();
    }
}
