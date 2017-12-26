package cn.orangeiot.job.verticle;

import cn.orangeiot.job.conf.JobConf;
import cn.orangeiot.job.verifycode.VerifyCodeJob;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.zookeeper.ZookeeperClusterManager;
import org.apache.commons.io.IOUtils;
import org.quartz.*;
import org.quartz.impl.JobDetailImpl;
import org.quartz.impl.StdSchedulerFactory;

import java.io.InputStream;
import java.util.Properties;

import static org.quartz.JobBuilder.newJob;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-12-18
 */
public class JobVerticle extends AbstractVerticle {


    private Properties properties;

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        loadClusting();
        startFuture.complete();
    }


    /**
     * @Description 加载配置
     * @author zhang bo
     * @date 17-12-18
     * @version 1.0
     */
    public void loadClusting() {
        InputStream jobIn = JobVerticle.class.getResourceAsStream("/quartz.properties");
        try {
            properties = new Properties();
            properties.load(jobIn);
            /**
             * 加载zk config
             */
            InputStream zkIn = JobVerticle.class.getResourceAsStream("/zkConf.json");
            InputStream configIn = JobVerticle.class.getResourceAsStream("/config.json");//全局配置
            String zkConf = "";//jdbc连接配置
            String config = "";
            zkConf = IOUtils.toString(zkIn, "UTF-8");//获取配置
            config = IOUtils.toString(configIn, "UTF-8");

            if (!zkConf.equals("")) {
                JsonObject json = new JsonObject(zkConf);
                JsonObject configJson = new JsonObject(config);

                System.setProperty("vertx.zookeeper.hosts", json.getString("hosts.zookeeper"));
                ClusterManager mgr = new ZookeeperClusterManager(json);
                VertxOptions options = new VertxOptions().setClusterManager(mgr);
//                options.setClusterHost(configJson.getString("host"));//本机地址

                //集群
                Vertx.clusteredVertx(options, rs -> {
                      if(rs.failed()){
                          rs.cause().printStackTrace();
                      }else {
                          vertx=rs.result();
                          //创建scheduler
                          SchedulerFactory schedulerFactory = null;
                          try {
                              schedulerFactory = new StdSchedulerFactory(properties);
                              Scheduler scheduler = schedulerFactory.getScheduler();

                              JobConf jobConf=new JobConf(vertx,configJson,scheduler);//配置任务调度
                              jobConf.verifyCode();

                              scheduler.start(); //启动
                          } catch (SchedulerException e) {
                              e.printStackTrace();
                          }
                      }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


        @Override
        public void stop (Future < Void > stopFuture) throws Exception {
            super.stop(stopFuture);
        }
}
