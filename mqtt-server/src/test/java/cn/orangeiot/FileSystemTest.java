package cn.orangeiot;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.FileSystem;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-10-19
 */
public class FileSystemTest extends AbstractVerticle {

    private static Map<Integer, AsyncFile> indexMap = new HashMap<>();

    private static Map<Integer, AsyncFile> logMap = new HashMap<>();

    private static AtomicInteger atomicInteger = new AtomicInteger(0);

    @SuppressWarnings("Duplicates")
    @Override
    public void start(Future<Void> startFuture) throws Exception {
        FileSystem fileSystem = vertx.fileSystem();
//        for (int i = 0; i < 10000; i++) {
//            fileSystem.createFile("/home/linuxzhangbo/file/" + i + ".log", null);
//            fileSystem.createFile("/home/linuxzhangbo/file/" + i + ".index", null);
//        }


        JsonObject jsonObject = new JsonObject().put("func", "bindGatewayByUser").put("uid", "5902aca835736f21ae1e7a82").put("devuuid", "IO01181911117");

        JsonObject jsonObject1 = new JsonObject().put("func", "gatewayBindList").put("uid", "5902aca835736f21ae1e7a82");


//        for (int i = 0; i < 10000; i++) {
//            AsyncFile indexFile = fileSystem.openBlocking("/home/linuxzhangbo/file/" + i + ".index", new OpenOptions().setAppend(true).setRead(true).setWrite(true));
//            AsyncFile logFile = fileSystem.openBlocking("/home/linuxzhangbo/file/" + i + ".log", new OpenOptions().setAppend(true).setRead(true).setWrite(true));
//            indexMap.put(i, indexFile);
//            logMap.put(i, logFile);
//        }


//        vertx.setTimer(10000, rs -> {
//            indexMap.get(1).write(Buffer.buffer().appendShort((short) 1).appendInt(jsonObject.toString().length()), 6, ars -> {
//                if (ars.failed()) {
//                    ars.cause().printStackTrace();
//                } else {
//                    logMap.get(1).write(Buffer.buffer().appendString(jsonObject.toString()));
//                }
//            });

//            final int indexMaxSize = 6 * 65535;//索引文件最大值
//
//            int
//
//            indexMap.get(1).read(Buffer.buffer(6), 0, 0, 6, ars -> {
//                if (ars.failed()) {
//                    ars.cause().printStackTrace();
//                } else {
//                    int messageId = ars.result().getShort(0);
//                    int offset = ars.result().getInt(2);
//                    logMap.get(1).read(Buffer.buffer(offset), 0, 0, offset, returndata -> {
//                        if (returndata.failed()) {
//                            returndata.cause().printStackTrace();
//                        } else {
//                            System.out.println(returndata.result().toString());
//                        }
//                    });
//                }
//            });
//        });


        boolean[] cc = new boolean[65535];

    }

    final int middle = 32768;

    public void valid(boolean[] arrs) {
        if (arrs.length != 65535) return;
        AtomicInteger atomicInteger = new AtomicInteger(0);
        int head = 0, tail = arrs.length - 1;
        while (head < tail) {
            if (arrs[head] == true) {
//                System.out.println("head -> " + head);
            }
            if (arrs[tail] == true) {
//                System.out.println("tail -> " + tail);
            }
            head++;
            tail--;
//            System.out.println(atomicInteger.incrementAndGet());
        }
        if (arrs[middle] == true) {
//            System.out.println("middle -> " + middle);
        }
    }

    public static void main(String[] args) {
//        Vertx.vertx().deployVerticle(FileSystemTest.class.getName());
        long startTime = System.currentTimeMillis();
        FileSystemTest fileSystemTest = new FileSystemTest();
        for (int j = 0; j < 100000; j++) {
            boolean[] arrs = new boolean[65535];
            for (int i = 0; i < 65535; i++) {
                boolean currentVal = new Random().nextBoolean();
                arrs[i] = currentVal;
            }
            fileSystemTest.valid(arrs);
        }
        System.out.println("process time -> " + (System.currentTimeMillis() - startTime));
    }
}
