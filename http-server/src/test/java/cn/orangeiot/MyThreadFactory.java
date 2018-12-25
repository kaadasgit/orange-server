package cn.orangeiot;

import io.vertx.core.impl.BlockedThreadChecker;
import javafx.concurrent.Worker;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-05-28
 */
public class MyThreadFactory implements ThreadFactory {


    private final String prefix;
    private final AtomicInteger threadCount = new AtomicInteger(0);
    private final boolean worker;
    private final long maxExecTime;

    public MyThreadFactory(String prefix, boolean worker, long maxExecTime) {
        this.prefix = prefix;
        this.worker = worker;
        this.maxExecTime = maxExecTime;
    }

    @Override
    public Thread newThread(Runnable r) {

        return null;
    }



}
