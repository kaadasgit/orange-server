package cn.orangeiot;

import org.bson.BsonObjectId;

import java.util.HashMap;
import java.util.Map;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-03-19
 */
public class PartTest {

    public static void main(String[] args) {
        final int parts = 16;//16个存储区

        Map<Integer, Object> map = new HashMap<>();

        for (int i = 0; i < 10000000; i++) {
            BsonObjectId bsonObjectId = new BsonObjectId();
            String str = "app:" + bsonObjectId.getValue();
            int shard = Time33(str) % 16;
            map.put(shard, "");
        }

        map.forEach((k, v) -> System.out.println(k + " , "));

    }


    public static int Time33(String str) {
        int len = str.length();
        int hash = 0;
        for (int i = 0; i < len; i++)
            // (hash << 5) + hash 相当于 hash * 33
            hash = (hash << 5) + hash + (int) str.charAt(i);
        return Math.abs(hash);
    }
}
