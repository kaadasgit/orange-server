package cn.orangeiot.common.model;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-01-25
 */
public class SNEntityModel {

    private int productNum;//生产数量

    private int batch;//生产批次

    private int startCount;//开始条数

    private String factory;//工厂代码

    private String model;//产品型号

    private String weekCode;//周代码

    private String yearCode;//年代码

    private String childCode;//子代码


    public String getChildCode() {
        return this.childCode;
    }

    public SNEntityModel setChildCode(String childCode) {
        this.childCode = childCode;
        return this;
    }

    public int getProductNum() {
        return this.productNum;
    }


    public SNEntityModel setProductNum(int productNum) {
        this.productNum = productNum;
        return this;
    }

    public int getBatch() {
        return this.batch;
    }

    public SNEntityModel setBatch(int batch) {
        this.batch = batch;
        return this;
    }

    public int getStartCount() {
        return this.startCount;
    }

    public SNEntityModel setStartCount(int startCount) {
        this.startCount = startCount;
        return this;
    }

    public String getFactory() {
        return this.factory;
    }

    public SNEntityModel setFactory(String factory) {
        this.factory = factory;
        return this;
    }

    public String getModel() {
        return this.model;
    }

    public SNEntityModel setModel(String model) {
        this.model = model;
        return this;
    }

    public String getWeekCode() {
        return this.weekCode;
    }

    public SNEntityModel setWeekCode(String weekCode) {
        this.weekCode = weekCode;
        return this;
    }

    public String getYearCode() {
        return this.yearCode;
    }

    public SNEntityModel setYearCode(String yearCode) {
        this.yearCode = yearCode;
        return this;
    }
}
