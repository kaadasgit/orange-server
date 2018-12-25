package cn.orangeiot;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestOptions;
import io.vertx.ext.unit.TestSuite;
import io.vertx.ext.unit.report.ReportOptions;

import java.util.Arrays;
import java.util.Objects;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-04-09
 */
public class JsonDataUpgradeTest {

    public static void main(String[] args) {
        new JsonDataUpgradeTest().run();
    }


    Vertx vertx;
    String srcPath = "/home/linuxzhangbo/publihs/kdsNormalLock.json";
    String dstPath = "/home/linuxzhangbo/publihs/kdsNormalLockResult.json";
    String versionType = "vendors";

    public void run() {
        TestOptions options = new TestOptions().addReporter(new ReportOptions().setTo("console"));
        TestSuite suite = TestSuite.create(JsonDataUpgradeTest.class.getName());


        suite.test("my_test_case", context -> {
            vertx = Vertx.vertx();
            vertx.fileSystem().open(srcPath, new OpenOptions().setCreateNew(false).setCreate(false).setRead(true), rs -> {
                if (rs.failed()) {
                    rs.cause().printStackTrace();
                    vertx.close(context.asyncAssertFailure());
                } else {
                    rs.result().setReadBufferSize(100000000);//讀取緩衝區的大小
                    rs.result().handler(buffer -> {
                        Buffer bf = Buffer.buffer();
                        Arrays.stream(buffer.toString().split("\n")).forEach(e -> {
                            JsonObject jsonObject = new JsonObject(e);
                            if (Objects.nonNull(jsonObject.getValue("adminname"))
                                    && !jsonObject.getString("adminname").trim().equals("") &&
                                    !jsonObject.getString("adminname")
                                            .matches("\\w+(\\.\\w)*@\\w+(\\.\\w{2,3}){1,3}")) { //检验是否是合法的邮箱地址)
                                if (!jsonObject.getString("adminname").startsWith("86")) {
                                    jsonObject.put("adminname", "86" + jsonObject.getString("adminname")).toString();
                                }

                            }
//                            if(Objects.nonNull((jsonObject.getValue("userTel")))){
//                                jsonObject.put("userTel", "86" + jsonObject.getString("userTel")).toString();
//                            }
                            if (Objects.nonNull(jsonObject.getValue("uname"))
                                    && !jsonObject.getString("uname").trim().equals("") &&
                                    !jsonObject.getString("uname")
                                            .matches("\\w+(\\.\\w)*@\\w+(\\.\\w{2,3}){1,3}")) { //检验是否是合法的邮箱地址)
                                if (!jsonObject.getString("uname").startsWith("86")) {
                                    jsonObject.put("uname", "86" + jsonObject.getString("uname")).toString();
                                }

                            }
                            bf.appendString(jsonObject.toString() + "\n");
                        });
                        vertx.fileSystem().writeFile(dstPath, bf, writeBuffer -> {
                            if (writeBuffer.failed()) {
                                writeBuffer.cause().printStackTrace();
                                vertx.close(context.asyncAssertFailure());
                            } else {
                                rs.result().close();
                                vertx.close();
                            }
                        });
                    });

                }
            });
        });

//        suite.after(rs ->
//                future.setHandler(e -> {
//                    if (e.result()) {
//                        vertx.close(rs.asyncAssertSuccess());
//                    } else {
//                        vertx.close(rs.asyncAssertFailure());
//                    }
//                })
//        );


        suite.run(options);
    }
}
