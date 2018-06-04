package cn.orangeiot;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-04-18
 */
public class DeadLoopClass {

    static{
        if(true){
            System.out.println(Thread.currentThread()+"init DeadLoopClass");
            while(true){

            }
        }
    }

}
