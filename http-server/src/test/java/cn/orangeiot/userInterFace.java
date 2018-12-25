package cn.orangeiot;

import java.lang.annotation.*;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-07-16
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface userInterFace {

    String url();

    String requsetMethod();
}
