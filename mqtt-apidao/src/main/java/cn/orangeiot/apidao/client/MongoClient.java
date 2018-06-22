package cn.orangeiot.apidao.client;

import com.mongodb.MongoCredential;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-11-23
 */
public class MongoClient {

    public static io.vertx.ext.mongo.MongoClient client;

    private static Logger logger = LogManager.getLogger(MongoClient.class);

    /**
     * @Description redisClient配置
     * @author zhang bo
     * @date 17-11-23
     * @version 1.0
     */
    public void mongoConf(Vertx vertx){
        InputStream mongoIn = MongoClient.class.getResourceAsStream("/mongo.json");
        String mongoConf = "";//jdbc连接配置
        try {
            mongoConf = IOUtils.toString(mongoIn, "UTF-8");//获取配置

            if (!mongoConf.equals("")) {
                JsonObject json = new JsonObject(mongoConf);
                client =io.vertx.ext.mongo.MongoClient.createShared(vertx,json);
            }
        }catch (Exception e){
            logger.error(e.getMessage(), e);
        }finally {
            if(null!=mongoIn)
                try {
                    mongoIn.close();
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
        }
    }
}
