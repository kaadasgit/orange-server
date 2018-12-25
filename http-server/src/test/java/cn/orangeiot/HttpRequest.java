package cn.orangeiot;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;

import static org.apache.http.Consts.UTF_8;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-04-11
 */
public class HttpRequest {

    public static void main(String[] args) {
        CloseableHttpClient httpclient = null;
        CloseableHttpResponse response = null;
        try {
            httpclient = new SSLClient();
        } catch (Exception e) {
//            logger.error(e.getMessage(), e);
        }
        try {
            String uri = "https://127.0.0.1:8090/user/edit/uploaduserhead";
//            String uri = "https://114.67.58.242:8090/user/edit/uploaduserhead";
            HttpPost httppost = new HttpPost(uri);

            String path = "/home/linuxzhangbo/图片/123.jpg";
            File file = new File(path);
            String fileType = path.substring(path.lastIndexOf(".") + 1, path.length());
//            FileBody bin = new FileBody(file, "image/" + fileType);
            FileBody bin = new FileBody(file, "image/" + fileType);
            HttpEntity reqEntity = MultipartEntityBuilder.create()
                    .addPart("file", bin)
//                    .addTextBody("uid ", "5ac9c9bc7e15d13f417b80a1")
                    .build();


            httppost.setHeader("token", "eyJfaWQiOiI1YWM5YzliYzdlMTVkMTNmNDE3YjgwYTEiLCJ1c2VybmFtZSI6InpiNTY0NzM5Nzg0QDE2My5jb20iLCJpYXQiOjE1MjMxNzU2MTB9");
            httppost.setHeader("uid", "5ac9c9bc7e15d13f417b80a1");
            httppost.setEntity(reqEntity);

            System.out.println("executing request " + httppost.getRequestLine());

            response = httpclient.execute(httppost);
            System.out.println("----------------------------------------");
            System.out.println(response.getStatusLine());
            HttpEntity resEntity = response.getEntity();
            if (resEntity != null) {
                System.out.println("Response content length: " + resEntity.getContentLength());
                Arrays.stream(response.getAllHeaders()).forEach(
                        e -> System.out.println("Response header key:" + e.getName() + "   ,value:" + e.getValue()));
                System.out.println("Response content: " + inputStream2String(resEntity.getContent()));
            }
            EntityUtils.consume(resEntity);

        } catch (Exception e) {
//            logger.error(e.getMessage(), e);

        } finally {
            try {
                if(response!=null){
                    response.close();
                    httpclient.close();
                }
            } catch (IOException e) {
//                logger.error(e.getMessage(), e);
            }
        }

    }


    public static String inputStream2String(InputStream in) throws IOException {
        StringBuffer out = new StringBuffer();
        byte[] b = new byte[4096];
        for (int n; (n = in.read(b)) != -1; ) {
            out.append(new String(b, 0, n));
        }
        return out.toString();
    }
}
