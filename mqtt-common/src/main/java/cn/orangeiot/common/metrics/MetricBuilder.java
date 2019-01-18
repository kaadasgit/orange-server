package cn.orangeiot.common.metrics;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author : baijun
 * @date : 2019-01-18
 * @description : Metric 函数集
 */
public enum MetricBuilder {

    /**
     * 处理计数类相关操作
     */
    COUNT {
        public void dec(Class clazz,OpsType metricType) {
            String strName = MetricRegistry.name(clazz,metricType.getDesc());
            if(metricOps.containsKey(strName)){
                ((Counter)metricOps.get(strName)).dec();
            }
        }

        public void inc(Class clazz,OpsType metricType) {
            String strName = MetricRegistry.name(clazz,metricType.getDesc());
            if(metricOps.containsKey(strName)) {
                ((Counter)metricOps.get(strName)).inc();
            }
        }
    };

    private final static ConcurrentHashMap<String, Object> metricOps = new ConcurrentHashMap<String, Object>();
    private final static MetricRegistry metricRegistry = new MetricRegistry();

    /**
     * 创建计数器
     * @param clazz 类对象
     * @param name 描述名
     */
    public MetricBuilder createCounter(Class clazz,String name) {
        String strName = MetricRegistry.name(clazz,name);
        if(!metricOps.containsKey(strName)) {
            Counter counter = metricRegistry.counter(strName);
            metricOps.put(strName,counter);
        }

        return this;
    }

    /**
     * reporter 启动器
     * @param metricLogName log4j配置对象
     * @param period 周期
     * @param timeUnit 时间类型
     */
    public  void start(String metricLogName,int period,TimeUnit timeUnit) {
        LoggerReporter reporter = LoggerReporter.forRegistry(metricRegistry,metricLogName).build();
        reporter.start(period,timeUnit);
    }

    public void dec(Class clazz,OpsType metricType) { throw new AbstractMethodError(); }

    public void inc(Class clazz,OpsType metricType) { throw new AbstractMethodError(); }

}
