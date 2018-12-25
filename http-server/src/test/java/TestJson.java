import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.json.JsonObject;

import java.util.Arrays;
import java.util.Objects;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-08-30
 */
public class TestJson {
    Vertx vertx;
    String srcPath = "/home/linuxzhangbo/user.json";
    String dstPath = "/home/linuxzhangbo/user4.json";



    public void run(){
        vertx = Vertx.vertx();
        vertx.fileSystem().open(srcPath, new OpenOptions().setCreateNew(false).setCreate(false).setRead(true), rs -> {
            if (rs.failed()) {
                rs.cause().printStackTrace();
            } else {
                rs.result().setReadBufferSize(10000000);//讀取緩衝區的大小
                rs.result().handler(buffer -> {
                    Buffer bf = Buffer.buffer();
                    Arrays.stream(buffer.toString().split("\n")).forEach(e -> {
                        JsonObject jsonObject = new JsonObject(e);
                        jsonObject.remove("_id");
                        jsonObject.remove("lockNickName");
                        jsonObject.remove("versionType");
                        jsonObject.remove("nickName");
                        jsonObject.remove("open_purview");

                        if(!Objects.nonNull(jsonObject.getValue("user_num"))){//APP开锁
                            jsonObject.put("开锁类型","APP");
                            jsonObject.put("帐号",jsonObject.getString("uname"));
                        }else {
                            jsonObject.put("开锁类型",jsonObject.getString("open_type"));
                            jsonObject.put("锁编号",jsonObject.getString("user_num"));
                        }

                        jsonObject.put("开门时间",jsonObject.getString("open_time"));
                        jsonObject.put("锁标识",jsonObject.getString("lockName"));
                        jsonObject.remove("open_type");
                        jsonObject.remove("uname");
                        jsonObject.remove("user_num");
                        jsonObject.remove("open_time");
                        jsonObject.remove("lockName");

                        bf.appendString(jsonObject.toString() + "\n");
                    });

                    vertx.fileSystem().writeFile(dstPath, bf, writeBuffer -> {
                        if (writeBuffer.failed()) {
                            writeBuffer.cause().printStackTrace();
                        } else {
                            rs.result().close();
                            System.out.println("ok");
                        }
                    });
                });
            }
        });
    }

    public static void main(String[] args) {
        new TestJson().run();
    }
}
