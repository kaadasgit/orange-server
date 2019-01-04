package cn.orangeiot.common.annotation.http;

import io.vertx.core.json.JsonObject;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

/**
 * Unit test for simple App.
 */
public class HttpZkFactoryTest
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public HttpZkFactoryTest(String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( HttpZkFactoryTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp()
    {
        assertTrue( true );
    }

    public void test001_handleHttpManagentMsg() {
        CuratorFramework curatorFramework = CuratorFrameworkFactory.newClient("127.0.0.1:2181",new ExponentialBackoffRetry(3000,3));
        curatorFramework.start();

        int port = 17000;
        String path = "/io.vertx/zk/http-managent";
        Class aClass = TestApi.class;

        HttpZkFactory.Instance.handleHttpManagentMsg(port,path,aClass,curatorFramework,CreateMode.EPHEMERAL);

        System.out.println("end");
    }

    public void test003_getHttpManagentMsg() throws Exception {
        CuratorFramework curatorFramework = CuratorFrameworkFactory.newClient("127.0.0.1:2181",new ExponentialBackoffRetry(3000,3));
        curatorFramework.start();

        String string = new String(curatorFramework.getData().forPath("/io.vertx/zk/http-managent/192.168.2.201:17000"));

        JsonObject jsonObject = new JsonObject(string);
        System.out.println(jsonObject.encodePrettily());
    }

    public void test002_handleHttpServerMsg() {
        CuratorFramework curatorFramework = CuratorFrameworkFactory.newClient("127.0.0.1:2181",new ExponentialBackoffRetry(3000,3));
        curatorFramework.start();

        int port = 8090;
        String path = "/io.vertx/zk/http-server";
        Class aClass = TestApi.class;

        HttpZkFactory.Instance.handleHttpServerMsg(port,path,aClass,curatorFramework, CreateMode.EPHEMERAL);
    }

    public void test004_getHttpServerMsg() {
        CuratorFramework curatorFramework = CuratorFrameworkFactory.newClient("127.0.0.1:2181",new ExponentialBackoffRetry(3000,3));
        curatorFramework.start();

        String string = null;
        try {
            string = new String(curatorFramework.getData().forPath("/io.vertx/zk/http-server/192.168.2.201:8090"));
            JsonObject jsonObject = new JsonObject(string);
            System.out.println(jsonObject.encodePrettily());
        } catch (Exception e) {
            e.printStackTrace();
        }
//        System.out.println(string);
    }
}
