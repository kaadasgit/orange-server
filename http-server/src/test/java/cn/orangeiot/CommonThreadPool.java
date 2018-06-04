package cn.orangeiot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-05-25
 */
public class CommonThreadPool <Job extends Runnable> implements ThreadPool<Job> {

    // 线程池最大限制数
    private static final int MAX_WORKER_NUMBERS = 100;
    // 线程池默认的数量
    private static final int DEFAULT_WORKER_NUMBERS = 1;
    // 线程池最小数量
    private static final int MIN_WORKER_NUMBERS = 1;
    // 工作列表
    private final LinkedList<Job> jobs = new LinkedList<Job>();
    // 工作者列表,synchronizedList的线程安全功能仅仅指的是如果直接使用它提供的函数，比如：queue.add(obj); 或者
    // queue.poll(obj);，这样我们自己不需要做任何同步。
    private final List<Worker> workers = Collections
            .synchronizedList(new ArrayList<Worker>());
    // 工作者线程数量
    // private int workerNum=DEFAULT_WORKER_NUMBERS;
    // 线程编号
    private AtomicLong threadNum = new AtomicLong();

    public CommonThreadPool() {
        initializeWokers(DEFAULT_WORKER_NUMBERS);
    }

    /**
     * 初始化线程池
     */
    private void initializeWokers(int num) {
        // 创建多个线程，加入workers中，并启动
        for (int i = 0; i < num; i++) {
            Worker worker = new Worker();
            workers.add(worker);
            Thread thread = new Thread(worker, "ThreadPool-Worker-"
                    + threadNum.getAndIncrement());
            thread.start();
        }
    }

    @Override
    public void execute(Job job) {
        if (job == null)
            return;
        synchronized (jobs) {
            jobs.addLast(job);
            jobs.notify();
        }
    }

    @Override
    public void shutdown() {
        for (Worker worker : workers) {
            worker.shutdown();
        }
    }

    @Override
    public void addWorkers(int num) {
        synchronized (jobs) {
            int size = workers.size();
            if (num + size > MAX_WORKER_NUMBERS) {// 添加后的大小大于最大值
                num = MAX_WORKER_NUMBERS - size;// 计算要新增的worker数量
            }
            initializeWokers(num);// 初始化这num个worker
        }
    }

    @Override
    public void removeWorker(int num) {
        synchronized (jobs) {
            if (num >= this.workers.size()) {
                throw new IllegalArgumentException("beyond workNum!");
            }
            int count = 0;
            while (count < num) {
                Worker worker = workers.get(count);
                if (workers.remove(worker)) {
                    worker.shutdown();
                    count++;
                }
            }

        }
    }

    @Override
    public int getJobSize() {
        // TODO Auto-generated method stub
        return jobs.size();
    }

    class Worker implements Runnable {

        private volatile boolean running = true;

        public void shutdown() {
            running = false;
        }

        @Override
        public void run() {

            while (running) {
                Job job = null;
                synchronized (jobs) {
                    while (jobs.isEmpty()) {// 如果jobs是空的，则执行jobs.wait，使用while而不是if，因为wait后可能已经为空了，继续等待
                        try {
                            jobs.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            Thread.currentThread().interrupt();// 中断
                            return;// 结束
                        }
                    }
                    job = jobs.removeFirst();// 第一个job
                    if (job != null) {
                        try {
                            job.run();//注意，这里是run而不是start，传入的Job
                        } catch (Exception e) {
                            // 忽略Job执行中的Exception
                            e.printStackTrace();
                        }
                    }
                }
            }

        }

    }
}
