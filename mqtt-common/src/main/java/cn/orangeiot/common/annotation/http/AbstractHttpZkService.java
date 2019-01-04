package cn.orangeiot.common.annotation.http;

import cn.orangeiot.common.annotation.KdsHttpMessage;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.curator.framework.CuratorFramework;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author : baijun
 * @date : 2019-01-04
 * @description : 抽象类
 */
public abstract  class AbstractHttpZkService implements HttpZkService{
    private static Logger logger = LogManager.getLogger(AbstractHttpZkService.class);

    protected String host; // http 服务 host
    protected int port; // http 服务 端口
    protected String urls; // http 服务 urls

    protected CuratorFramework curatorFramework; // zk 客户端

    protected String path; // 节点路径
    protected String actualPath; // 最后节点路径

    protected Class aClass; // 接口类

    protected CreateMode createMode; // 节点的类型

    @Override
    public void createHost() {
        try {
            InetAddress inetAddress = InetAddress.getLocalHost();
            this.host = inetAddress.getHostAddress();
        } catch (UnknownHostException e) {
//            e.printStackTrace();
            logger.error(e);
        }
    }

    @Override
    public void createUrls() {
        JsonArray jsonArray = new JsonArray();
        JsonObject jsonObject = null;

        Field[] fields = aClass.getDeclaredFields();

        try {
            for (Field  field : fields) {
                field.setAccessible(true);
                if(field.isAnnotationPresent(KdsHttpMessage.class)) {
                    KdsHttpMessage kdsHttpMessage = field.getAnnotation(KdsHttpMessage.class);
                    jsonObject = new JsonObject();
                    jsonObject.put("url",field.get(this.aClass)).put("method",kdsHttpMessage.Method());
                    jsonArray.add(jsonObject);
                }
            }
        } catch (Exception e) {
//            e.printStackTrace();
            logger.error(e);
        }

        this.urls = jsonArray.encode();
    }

    @Override
    public boolean exists() {
        boolean result = false;
        try {
            StringBuilder builder = new StringBuilder(path);
            builder.append("/").append(host).append(":").append(port);

            this.actualPath = builder.toString();
            Stat stat = curatorFramework.checkExists().forPath(this.actualPath);
            result = stat != null;
        } catch (Exception e) {
//            e.printStackTrace();
            logger.error(e);
        }
        return result;
    }

    @Override
    public String create() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.put("host",this.host).put("port",this.port).put("urls",this.urls);

        return  jsonObject.encode();
    }

    @Override
    public void save() {
        try {
            // zookeeper 节点路径不存在，创建
            if(!exists()) {
                curatorFramework.create().creatingParentsIfNeeded().withMode(this.createMode).forPath(this.actualPath,create().getBytes());
            }
            // zookeeper 节点路径已经存在，更新
            else {
                curatorFramework.setData().forPath(this.actualPath,create().getBytes());
            }
        } catch (Exception e) {
//            e.printStackTrace();
            logger.error(e);
        }
    }
}
