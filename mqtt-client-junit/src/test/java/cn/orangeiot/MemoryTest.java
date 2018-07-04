package cn.orangeiot;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-07-03
 */
public class MemoryTest {


    public static Map<String, String> map = new ConcurrentHashMap();

    public static void main(String[] args) {
        Long startTime = System.currentTimeMillis();

        for (int i = 0; i < 10000000; i++) {
            map.put("1" + i, "zhang==" + i);
        }




        System.out.println("process time -> " + (System.currentTimeMillis() - startTime) + " , size ->" + map.size());


        while (true) {

        }


    }
}
