package cn.orangeiot.mqtt.log.handler.entity;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-10-23
 */
public class IndexEntity {

    private short msgId; // 消息Id

    private int startOffset;//開始的偏移量

    private int endOffset;//結束的偏移量

    private byte valid;//是否有效 0 有效 1 無效

    private long timestamp;//時間戳

    private byte partition;//區

    private long reSendTimerId;//重发器 唯一id

    private byte qos;//mqtt  qos

    public long getReSendTimerId() {
        return reSendTimerId;
    }

    public void setReSendTimerId(long reSendTimerId) {
        this.reSendTimerId = reSendTimerId;
    }

    public byte getQos() {
        return qos;
    }

    public void setQos(byte qos) {
        this.qos = qos;
    }

    public short getMsgId() {
        return msgId;
    }

    public void setMsgId(short msgId) {
        this.msgId = msgId;
    }

    public int getStartOffset() {
        return startOffset;
    }

    public void setStartOffset(int startOffset) {
        this.startOffset = startOffset;
    }

    public int getEndOffset() {
        return endOffset;
    }

    public void setEndOffset(int endOffset) {
        this.endOffset = endOffset;
    }

    public byte getValid() {
        return valid;
    }

    public void setValid(byte valid) {
        this.valid = valid;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public byte getPartition() {
        return partition;
    }

    public void setPartition(byte partition) {
        this.partition = partition;
    }
}
