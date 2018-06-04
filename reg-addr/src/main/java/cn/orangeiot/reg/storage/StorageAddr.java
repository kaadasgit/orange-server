package cn.orangeiot.reg.storage;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-05-16
 */
public interface StorageAddr {

    String PUT_STORAGE_DATA = "putStorageAddr";//存储数据

    String DELALL_STORAGE_DATA = "delAllStorageAddr";//移除所有数据

    String DEL_STORAGE_DATA = "delStorageAddr";//移除数据

    String GET_STORAGE_DATA = "getStorageAddr";//获取数据
}
