package cn.orangeiot.reg;


import cn.orangeiot.reg.adminlock.AdminlockAddr;
import cn.orangeiot.reg.event.EventAddr;
import cn.orangeiot.reg.file.FileAddr;
import cn.orangeiot.reg.gateway.GatewayAddr;
import cn.orangeiot.reg.log.LogAddr;
import cn.orangeiot.reg.memenet.MemenetAddr;
import cn.orangeiot.reg.message.MessageAddr;
import cn.orangeiot.reg.ota.OtaAddr;
import cn.orangeiot.reg.publish.PublishAddr;
import cn.orangeiot.reg.rateLimit.RateLimitAddr;
import cn.orangeiot.reg.storage.StorageAddr;
import cn.orangeiot.reg.user.UserAddr;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-12-12
 */
public interface EventbusAddr extends PublishAddr, MessageAddr, UserAddr, FileAddr,
        AdminlockAddr, MemenetAddr, GatewayAddr, EventAddr, OtaAddr, StorageAddr, LogAddr, RateLimitAddr {


}
