package cn.orangeiot;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-05-28
 */
public class ThreadExiedTest {


    public static void main(String[] args) {
        ExecutorService fixedThreadPool = Executors.newFixedThreadPool(3);
        for (int i = 0; i < 10; i++) {
            final int index = i;
            fixedThreadPool.execute(() -> {
                try {
                    System.out.println(index);
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    //  Auto-generated catch block
//                    logger.error(e.getMessage(), e);
                }
            });
        }
    }
}
