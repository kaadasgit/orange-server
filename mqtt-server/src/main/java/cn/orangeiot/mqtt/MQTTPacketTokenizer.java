package cn.orangeiot.mqtt;

import java.nio.ByteBuffer;
import java.util.LinkedHashSet;

/**
 * Created by Paolo Iddas.
 * MQTT Protocol tokenizer.
 */
public class MQTTPacketTokenizer {

    /* tokenizer state machine */
    public static enum MqttTokenizerState {
        WAITING_FIRST, WAITING_REMAINING_LENGTH, WAITING_PAYLOAD
    }

    /* */
    private MqttTokenizerState tokenizerState = MqttTokenizerState.WAITING_FIRST;

    public MqttTokenizerState getTokenizerState() {
        return tokenizerState;
    }

    /* */
    private final static int MAX_FIXED_HEADER_LENGTH = 5;
    private ByteBuffer fixedHeader = ByteBuffer.allocate(MAX_FIXED_HEADER_LENGTH);
    private int fixedHeaderLength = 0;
    /* */
    private int multiplier = 1;
    private int remainingTotalLength = 0;
    private int remainingCounter = 0;
    /* */
    private ByteBuffer tokenContainer;

    /* listener */
    public interface MqttTokenizerListener {
        void onToken(byte[] token, boolean timeout) throws Exception;
        void onError(Throwable e);
    }

    private LinkedHashSet<MqttTokenizerListener> listenerCollection = new LinkedHashSet<MqttTokenizerListener>();

    public MQTTPacketTokenizer() {
        init();
    }

    public void debugState() {
        System.out.println("STATE = " + getTokenizerState());
        System.out.println("FIXED HEADER LENGTH = " + fixedHeaderLength);
        System.out.println("PAYLOAD POSITION = " + remainingCounter);
        System.out.println("PAYLOAD LENGTH = " + remainingTotalLength);
    }

    public void init() {
        tokenizerState = MqttTokenizerState.WAITING_FIRST;
        fixedHeader.clear();
        fixedHeaderLength = 0;
        multiplier = 1;
        remainingTotalLength = 0;
        remainingCounter = 0;
    }

    protected int process(byte data) {
        switch (tokenizerState) {
            case WAITING_FIRST:

                tokenizerState = MqttTokenizerState.WAITING_REMAINING_LENGTH;
                fixedHeader.put(data);
                fixedHeaderLength++;
                break;

            case WAITING_REMAINING_LENGTH:
                tokenizerState = MqttTokenizerState.WAITING_REMAINING_LENGTH;
                fixedHeader.put(data);
                fixedHeaderLength++;
                /* check for continuation bit */
                remainingTotalLength += ((data & 127) * multiplier);
                multiplier *= 128;
                if ((data & 128) == 0 || fixedHeaderLength == MAX_FIXED_HEADER_LENGTH) {
                    tokenizerState = MqttTokenizerState.WAITING_PAYLOAD;
                    tokenContainer = ByteBuffer.allocate(fixedHeaderLength + remainingTotalLength);
                    tokenContainer.put(fixedHeader.array(), 0, fixedHeader.position());
                    if (remainingTotalLength == 0) {
                        complete(false);
                        init();
                    }
                }

                break;
            case WAITING_PAYLOAD:
                tokenContainer.put(data);
                remainingCounter++;
                if (remainingCounter == remainingTotalLength) {
                    tokenizerState = MqttTokenizerState.WAITING_FIRST;
                    complete(false);
                    init();
                }
                break;
            default:
                break;
        }

        return 1;
    }

    protected int process(byte[] data, int offset, int length) {
        int processed = -1;
        switch (tokenizerState) {
            case WAITING_FIRST:
                processed = process(data[offset]);
                break;
            case WAITING_REMAINING_LENGTH:
                processed = process(data[offset]);
                break;

            case WAITING_PAYLOAD:
                processed = length - offset;
                int remaining = remainingTotalLength - remainingCounter;
                processed = processed < remaining ? processed : remaining;
                if (remainingCounter + processed <= remainingTotalLength) {
                    tokenContainer.put(data, offset, processed);
                    remainingCounter += processed;
                    if (remainingCounter == remainingTotalLength) {
                        tokenizerState = MqttTokenizerState.WAITING_FIRST;
                        complete(false);
                        init();
                    }
                }
                break;
            default:
                break;
        }
        return processed;
    }

    public MqttTokenizerState process(byte[] data) {
        int processed = 0;
        while (processed < data.length) {
            int temp = process(data, processed, data.length);
            processed += temp;
            if (temp == -1) {
                break;
            }
        }
        return getTokenizerState();
    }

    public void registerListener(MqttTokenizerListener listener) {
        listenerCollection.add(listener);
    }

    public void removeListener(MqttTokenizerListener listener) {
        listenerCollection.remove(listener);
    }

    public void removeAllListeners() {
        listenerCollection.clear();
    }

    private void notifyListeners(byte[] token, boolean timeout) {

        for (MqttTokenizerListener l : listenerCollection) {
            try {
                l.onToken(token, timeout);
            } catch (Throwable e) {
                l.onError(e);
            }
        }
    }

    private void complete(boolean timeout) {
        try {
            byte[] aByteArray = tokenContainer.array();
            this.notifyListeners(aByteArray, timeout);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
