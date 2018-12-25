package cn.orangeiot.event.service;

import java.io.Serializable;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-07-17
 */
public interface BaseService<T>{

    T handler();
}
