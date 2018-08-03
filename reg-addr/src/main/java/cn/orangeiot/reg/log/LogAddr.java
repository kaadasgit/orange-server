package cn.orangeiot.reg.log;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-07-26
 */
public interface LogAddr {

    String WRITE_LOG="writeLog";//写入日志

    String READ_LOG="readLog";//读取日志

    String CONSUME_LOG="consumeLog";//消费日志

    String MSG_EXISTS="msgExists";//消息是否存在

    String SAVE_PUBREL="savePubRel";//存储pubrel消息id

    String SEND_PUBREL="sendPubRel";//推送pubrel包

    String CONSUME_PUBREL="consumPubRel";//消费释放包
}
