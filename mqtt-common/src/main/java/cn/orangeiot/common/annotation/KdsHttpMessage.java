package cn.orangeiot.common.annotation;

import java.lang.annotation.*;

@Documented
@Inherited
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface KdsHttpMessage {
    public String Method() default "";
}
