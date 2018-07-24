package cn.orangeiot;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-04-18
 */
public class DeadTest {

    public static void main(String[] args) {
        Runnable script=new Runnable() {
            @Override
            public void run() {
                System.out.println(Thread.currentThread()+"start");
                DeadLoopClass deadLoopClass=new DeadLoopClass();
                System.out.println(Thread.currentThread()+"run over");
            }
        };

        Thread thread1=new Thread(script);
        Thread thread2=new Thread(script);
        thread1.start();
        thread2.start();
    }
}
