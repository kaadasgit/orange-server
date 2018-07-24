package cn.orangeiot.apidao.client;

import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Environments;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-05-16
 */
public class StorageClient {

    static Environment env;


    public static Environment getEnv() {
        return env;
    }

    /**
     * @Description 加载Storage配置
     * @author zhang bo
     * @date 18-5-16
     * @version 1.0
     */
    public void loadConf(String path) {
        env = Environments.newInstance(path);
    }
}
