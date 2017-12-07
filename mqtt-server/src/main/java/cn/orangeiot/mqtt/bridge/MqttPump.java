package cn.orangeiot.mqtt.bridge;

import cn.orangeiot.mqtt.MQTTPacketTokenizer;
import cn.orangeiot.mqtt.parser.MQTTDecoder;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.Pump;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;

/**
 * Created by Giovanni Baleani on 07/09/2015.
 */
public class MqttPump implements Pump {

    private final ReadStream<Buffer> readStream;
    private final WriteStream<Buffer> writeStream;
    private final Handler<Buffer> dataHandler;
    private final Handler<Void> drainHandler;
    private int pumped;
    private MQTTPacketTokenizer t;
    private Listener listener;

    /**
     * Create a new {@code Pump} with the given {@code ReadStream} and {@code WriteStream}. Set the write queue max size
     * of the write stream to {@code maxWriteQueueSize}
     */
    public MqttPump(ReadStream<Buffer> rs, WriteStream<Buffer> ws, int maxWriteQueueSize) {
        this(rs, ws);
        this.writeStream.setWriteQueueMaxSize(maxWriteQueueSize);
    }

    public MqttPump(ReadStream<Buffer> rs, WriteStream<Buffer> ws) {
        this.readStream = rs;
        this.writeStream = ws;
        drainHandler = v-> readStream.resume();
        dataHandler = data -> {
            t.process(data.getBytes());
        };
        t = new MQTTPacketTokenizer();
        t.registerListener(new MQTTPacketTokenizer.MqttTokenizerListener() {
            @Override
            public void onToken(byte[] token, boolean timeout) throws Exception {
                try {
                    Buffer b = Buffer.buffer(token);
                    new MQTTDecoder().dec(b);
                    writeStream.write(b);
                    incPumped();
                    if (writeStream.writeQueueFull()) {
                        readStream.pause();
                        writeStream.drainHandler(drainHandler);
                    }
                } catch (Throwable e) {
                    if(listener!=null)
                        listener.onError(e);
                }
            }

            @Override
            public void onError(Throwable e) {
                if(listener!=null)
                    listener.onError(e);
            }
        });
    }

    /**
     * Set the write queue max size to {@code maxSize}
     */
    @Override
    public MqttPump setWriteQueueMaxSize(int maxSize) {
        writeStream.setWriteQueueMaxSize(maxSize);
        return this;
    }

    /**
     * Start the Pump. The Pump can be started and stopped multiple times.
     */
    @Override
    public MqttPump start() {
        readStream.handler(dataHandler);
        return this;
    }

    /**
     * Stop the Pump. The Pump can be started and stopped multiple times.
     */
    @Override
    public MqttPump stop() {
        writeStream.drainHandler(null);
        readStream.handler(null);
        return this;
    }

    /**
     * Return the total number of elements pumped by this pump.
     */
    @Override
    public synchronized int numberPumped() {
        return pumped;
    }

    // Note we synchronize as numberPumped can be called from a different thread however incPumped will always
    // be called from the same thread so we benefit from bias locked optimisation which should give a very low
    // overhead
    private synchronized void incPumped() {
        pumped++;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    interface Listener {
        void onError(Throwable e);
    }
}

