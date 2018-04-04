package cn.orangeiot.common.utils;

import cn.orangeiot.common.model.SNEntityModel;
import org.apache.curator.shaded.com.google.common.primitives.Chars;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-01-25
 */
public class SNProductUtils {

    /**
     * @Description 模塊sn號生產
     * @author zhang bo
     * @date 18-1-25
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public static Map<String, Object> snProduct(SNEntityModel snEntityModel) {
        int final_num = 9999;
        boolean flag = (snEntityModel.getProductNum() + snEntityModel.getStartCount()) > final_num ? true : false;//余下条数
        int oneEndProNum = (snEntityModel.getProductNum() + snEntityModel.getStartCount()) > final_num
                ? snEntityModel.getProductNum() - (final_num - snEntityModel.getStartCount())
                : snEntityModel.getStartCount();//第一次生产剩余条数

        Map<String, Object> map = new HashMap<>();
        List<String> snList = new ArrayList<>();//生产的SN号集合
        int batchs = snEntityModel.getBatch();
        int rsCount = 0;//结果数
        if (flag) {//第一情情況超过最大值
            String week = snEntityModel.getWeekCode();
            String time = snEntityModel.getYearCode();
            //第一次区间生成
            for (int i = snEntityModel.getStartCount() + 1; i <= final_num; i++) {
                snList.add(snEntityModel.getModel() + snEntityModel.getFactory() + time + week + snEntityModel.getBatch() + val(i));
            }
            int page;//頁數
            int lastStartNum;//最後一次結束的條數
            if (oneEndProNum > final_num) {
                page = oneEndProNum % final_num == 0 ? oneEndProNum / final_num : oneEndProNum / final_num + 1;//一共多少批次
                lastStartNum = oneEndProNum - (final_num * (oneEndProNum / final_num));//最后次数的开始阶段
            } else {
                page = 1;//一共多少批次
                lastStartNum = oneEndProNum;
            }
            //余下区间生成
            for (int i = snEntityModel.getBatch() + 1; i <= (page + snEntityModel.getBatch()); i++) {
                if (i == (page + snEntityModel.getBatch())) //最後一次折疊的餘數
                    for (int j = 1; j <= lastStartNum; j++) {
                        snList.add(snEntityModel.getModel() + snEntityModel.getFactory() + time + week + i + val(j));

                    }
                else
                    for (int j = 1; j <= final_num; j++) {
                        snList.add(snEntityModel.getModel() + snEntityModel.getFactory() + time + week + i + val(j));
                    }
                batchs += 1;
            }
            rsCount = lastStartNum;
        } else {
            String week = snEntityModel.getWeekCode();
            String time = snEntityModel.getYearCode();
            for (int i = snEntityModel.getStartCount() + 1; i <= (snEntityModel.getStartCount() + snEntityModel.getProductNum()); i++) {
                snList.add(snEntityModel.getModel() + snEntityModel.getFactory() + time + week + snEntityModel.getBatch() + val(i));
            }
            rsCount = (snEntityModel.getStartCount() + snEntityModel.getProductNum());
        }
        map.put("snList", snList);
        map.put("batchs", batchs);
        map.put("rsCount", rsCount);
        return map;
    }

    @SuppressWarnings("Duplicates")
    public static String val(int count) {
        String str = "";
        switch (String.valueOf(count).length()) {
            case 1:
                str = "000" + String.valueOf(count);
                break;
            case 2:
                str = "00" + String.valueOf(count);
                break;
            case 3:
                str = "0" + String.valueOf(count);
                break;
            default:
                str = String.valueOf(count);
                break;
        }
        return str;
    }


    /**
     * @Description 模塊的寫入PN號
     * @author zhang bo
     * @date 18-1-25
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public static Map<String, Object> snDeviceInSN(SNEntityModel snEntityModel) {
        int final_num = 9999;
        boolean flag = (snEntityModel.getProductNum() + snEntityModel.getStartCount()) > final_num ? true : false;//余下条数
        int oneEndProNum = (snEntityModel.getProductNum() + snEntityModel.getStartCount()) > final_num
                ? snEntityModel.getProductNum() - (final_num - snEntityModel.getStartCount())
                : snEntityModel.getStartCount();//第一次生产剩余条数

        Map<String, Object> map = new HashMap<>();
        List<String> snList = new ArrayList<>();//生产的SN号集合
        int batchs = snEntityModel.getBatch();
        int rsCount = 0;//结果数
        if (flag) {//第一情情況超过最大值
            String week = snEntityModel.getWeekCode();
            String time = snEntityModel.getYearCode();
            //第一次区间生成
            for (int i = snEntityModel.getStartCount() + 1; i <= final_num; i++) {
                snList.add(snEntityModel.getModel() + snEntityModel.getChildCode() + time + week + isGeNum(snEntityModel.getBatch()) + val(i));
            }
            int page;//頁數
            int lastStartNum;//最後一次結束的條數
            if (oneEndProNum > final_num) {
                page = oneEndProNum % final_num == 0 ? oneEndProNum / final_num : oneEndProNum / final_num + 1;//一共多少批次
                lastStartNum = oneEndProNum - (final_num * (oneEndProNum / final_num));//最后次数的开始阶段
            } else {
                page = 1;//一共多少批次
                lastStartNum = oneEndProNum;
            }
            //余下区间生成
            for (int i = snEntityModel.getBatch() + 1; i <= (page + snEntityModel.getBatch()); i++) {
                if (i == (page + snEntityModel.getBatch())) //最後一次折疊的餘數
                    for (int j = 1; j <= lastStartNum; j++) {
                        snList.add(snEntityModel.getModel() + snEntityModel.getChildCode() + time + week + isGeNum(i) + val(j));

                    }
                else
                    for (int j = 1; j <= final_num; j++) {
                        snList.add(snEntityModel.getModel() + snEntityModel.getChildCode() + time + week + isGeNum(i) + val(j));
                    }
                batchs += 1;
            }
            rsCount = lastStartNum;
        } else {
            String week = snEntityModel.getWeekCode();
            String time = snEntityModel.getYearCode();
            for (int i = snEntityModel.getStartCount() + 1; i <= (snEntityModel.getStartCount() + snEntityModel.getProductNum()); i++) {
                snList.add(snEntityModel.getModel() + snEntityModel.getChildCode() + time + week + isGeNum(snEntityModel.getBatch()) + val(i));
            }
            rsCount = (snEntityModel.getStartCount() + snEntityModel.getProductNum());
        }
        map.put("snList", snList);
        map.put("batchs", batchs);
        map.put("rsCount", rsCount);
        return map;
    }


    /**
     * @Description
     * @author zhang bo
     * @date 18-3-22
     * @version 1.0
     */
    public static String isGeNum(int num) {
        String result;
        if (num > 9)
            result = String.valueOf((char) (64 + (num-9)));
        else
            result = String.valueOf(num);
        return result;
    }


    public static void main(String[] args) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yy"));//获取年代码
        int dayWeek = LocalDate.now().get(ChronoField.ALIGNED_WEEK_OF_YEAR);//获取周代码
        String week = String.valueOf(dayWeek).length() == 1 ? "0" + String.valueOf(dayWeek) : String.valueOf(dayWeek);

        Map map = snDeviceInSN(new SNEntityModel().setProductNum(100000)
                .setBatch(1).setStartCount(0).setModel("og").setChildCode("01")
                .setWeekCode(week).setYearCode(time));
        for (String str : (List<String>) map.get("snList")) {
            System.out.println("=========sn=>" + str);
        }
        System.out.println("=========size=>" + ((List<String>) map.get("snList")).size());
        System.out.println("=========batchs=>" + map.get("batchs"));
        System.out.println("=========rsCount=>" + map.get("rsCount"));
    }
}
