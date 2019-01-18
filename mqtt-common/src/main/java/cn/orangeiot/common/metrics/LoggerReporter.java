package cn.orangeiot.common.metrics;

import com.codahale.metrics.*;
import com.codahale.metrics.Timer;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.DateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

/**
 * @author : baijun
 * @date : 2019-01-18
 * @description : 自定义 Metrics Reporter,处理性能监控的日志
 */
public class LoggerReporter extends ScheduledReporter {

    private Logger logger; //日志对象
    private final Locale locale;
    private final Clock clock;
    private final DateFormat dateFormat;

    public static LoggerReporter.Builder forRegistry(MetricRegistry registry, String metricLogName) {
        return new LoggerReporter.Builder(registry,metricLogName);
    }

    public LoggerReporter(MetricRegistry registry, Logger logger, Locale locale, Clock clock, TimeZone timeZone, TimeUnit rateUnit, TimeUnit durationUnit, MetricFilter filter) {
        super(registry,"Logger-Reporter", filter, rateUnit,durationUnit);
        this.logger = logger;
        this.locale = locale;
        this.clock = clock;
        this.dateFormat = DateFormat.getDateTimeInstance(3,2,locale);
        this.dateFormat.setTimeZone(timeZone);
    }

    /**
     * 自定义实现日志的分类处理
     *
     * @param gauges
     * @param counters
     * @param histograms
     * @param meters
     * @param timers
     */
    @Override
    public void report(SortedMap<String, Gauge> gauges, SortedMap<String, Counter> counters, SortedMap<String, Histogram> histograms, SortedMap<String, Meter> meters, SortedMap<String, Timer> timers) {
        if(gauges.isEmpty() && counters.isEmpty() && histograms.isEmpty() && meters.isEmpty() && timers.isEmpty()) {
            return;
        }

        String dateTime = this.dateFormat.format(new Date(this.clock.getTime()));
        this.printWithBanner(dateTime, '=');
//        this.logger.info("\n");
        Iterator i$;
        Entry entry;
        if (!gauges.isEmpty()) {
            this.printWithBanner("-- Gauges", '-');
            i$ = gauges.entrySet().iterator();

            while(i$.hasNext()) {
                entry = (Entry)i$.next();
                this.logger.info((String)entry.getKey()+"\n");
                this.printGauge(entry);
            }

//            this.logger.info("\n");
        }

        if (!counters.isEmpty()) {
            this.printWithBanner("-- Counters", '-');
            i$ = counters.entrySet().iterator();

            while(i$.hasNext()) {
                entry = (Entry)i$.next();
                this.logger.info((String)entry.getKey()+"\n");
                this.printCounter(entry);
            }

//            this.logger.info("\n");
        }

        if (!histograms.isEmpty()) {
            this.printWithBanner("-- Histograms", '-');
            i$ = histograms.entrySet().iterator();

            while(i$.hasNext()) {
                entry = (Entry)i$.next();
                this.logger.info((String)entry.getKey()+"\n");
                this.printHistogram((Histogram)entry.getValue());
            }

//            this.logger.info("\n");
        }

        if (!meters.isEmpty()) {
            this.printWithBanner("-- Meters", '-');
            i$ = meters.entrySet().iterator();

            while(i$.hasNext()) {
                entry = (Entry)i$.next();
                this.logger.info((String)entry.getKey()+"\n");
                this.printMeter((Meter)entry.getValue());
            }

//            this.logger.info("\n");
        }

        if (!timers.isEmpty()) {
            this.printWithBanner("-- Timers", '-');
            i$ = timers.entrySet().iterator();

            while(i$.hasNext()) {
                entry = (Entry)i$.next();
                this.logger.info((String)entry.getKey()+"\n");
                this.printTimer((Timer)entry.getValue());
            }

//            this.logger.info("\n");
        }

//        this.logger.info("\n");
//        this.output.flush();
    }

    private void printMeter(Meter meter) {
        this.logger.printf(Level.INFO,"             count = %d%n", meter.getCount());
        this.logger.printf(Level.INFO, "         mean rate = %2.2f events/%s%n", this.convertRate(meter.getMeanRate()), this.getRateUnit());
        this.logger.printf(Level.INFO, "     1-minute rate = %2.2f events/%s%n", this.convertRate(meter.getOneMinuteRate()), this.getRateUnit());
        this.logger.printf(Level.INFO, "     5-minute rate = %2.2f events/%s%n", this.convertRate(meter.getFiveMinuteRate()), this.getRateUnit());
        this.logger.printf(Level.INFO, "    15-minute rate = %2.2f events/%s%n", this.convertRate(meter.getFifteenMinuteRate()), this.getRateUnit());
    }

    private void printCounter(Entry<String, Counter> entry) {
        this.logger.printf(Level.INFO, "             count = %d%n", ((Counter)entry.getValue()).getCount());
    }

    private void printGauge(Entry<String, Gauge> entry) {
        this.logger.printf(Level.INFO, "             value = %s%n", ((Gauge)entry.getValue()).getValue());
    }

    private void printHistogram(Histogram histogram) {
        this.logger.printf(Level.INFO, "             count = %d%n", histogram.getCount());
        Snapshot snapshot = histogram.getSnapshot();
        this.logger.printf(Level.INFO, "               min = %d%n", snapshot.getMin());
        this.logger.printf(Level.INFO, "               max = %d%n", snapshot.getMax());
        this.logger.printf(Level.INFO, "              mean = %2.2f%n", snapshot.getMean());
        this.logger.printf(Level.INFO, "            stddev = %2.2f%n", snapshot.getStdDev());
        this.logger.printf(Level.INFO, "            median = %2.2f%n", snapshot.getMedian());
        this.logger.printf(Level.INFO, "              75%% <= %2.2f%n", snapshot.get75thPercentile());
        this.logger.printf(Level.INFO, "              95%% <= %2.2f%n", snapshot.get95thPercentile());
        this.logger.printf(Level.INFO, "              98%% <= %2.2f%n", snapshot.get98thPercentile());
        this.logger.printf(Level.INFO, "              99%% <= %2.2f%n", snapshot.get99thPercentile());
        this.logger.printf(Level.INFO, "            99.9%% <= %2.2f%n", snapshot.get999thPercentile());
    }

    private void printTimer(Timer timer) {
        Snapshot snapshot = timer.getSnapshot();
        this.logger.printf(Level.INFO, "             count = %d%n", timer.getCount());
        this.logger.printf(Level.INFO, "         mean rate = %2.2f calls/%s%n", this.convertRate(timer.getMeanRate()), this.getRateUnit());
        this.logger.printf(Level.INFO, "     1-minute rate = %2.2f calls/%s%n", this.convertRate(timer.getOneMinuteRate()), this.getRateUnit());
        this.logger.printf(Level.INFO, "     5-minute rate = %2.2f calls/%s%n", this.convertRate(timer.getFiveMinuteRate()), this.getRateUnit());
        this.logger.printf(Level.INFO, "    15-minute rate = %2.2f calls/%s%n", this.convertRate(timer.getFifteenMinuteRate()), this.getRateUnit());
        this.logger.printf(Level.INFO, "               min = %2.2f %s%n", this.convertDuration((double)snapshot.getMin()), this.getDurationUnit());
        this.logger.printf(Level.INFO, "               max = %2.2f %s%n", this.convertDuration((double)snapshot.getMax()), this.getDurationUnit());
        this.logger.printf(Level.INFO, "              mean = %2.2f %s%n", this.convertDuration(snapshot.getMean()), this.getDurationUnit());
        this.logger.printf(Level.INFO, "            stddev = %2.2f %s%n", this.convertDuration(snapshot.getStdDev()), this.getDurationUnit());
        this.logger.printf(Level.INFO, "            median = %2.2f %s%n", this.convertDuration(snapshot.getMedian()), this.getDurationUnit());
        this.logger.printf(Level.INFO, "              75%% <= %2.2f %s%n", this.convertDuration(snapshot.get75thPercentile()), this.getDurationUnit());
        this.logger.printf(Level.INFO, "              95%% <= %2.2f %s%n", this.convertDuration(snapshot.get95thPercentile()), this.getDurationUnit());
        this.logger.printf(Level.INFO, "              98%% <= %2.2f %s%n", this.convertDuration(snapshot.get98thPercentile()), this.getDurationUnit());
        this.logger.printf(Level.INFO, "              99%% <= %2.2f %s%n", this.convertDuration(snapshot.get99thPercentile()), this.getDurationUnit());
        this.logger.printf(Level.INFO, "            99.9%% <= %2.2f %s%n", this.convertDuration(snapshot.get999thPercentile()), this.getDurationUnit());
    }

    private void printWithBanner(String s,char c) {
        StringBuilder buffer = new StringBuilder(s);
        buffer.append(' ');
        for(int i = 0; i < 80 - s.length() - 1; ++i) {
            buffer.append(c);
        }
        buffer.append("\n");

        logger.info(buffer.toString());
    }

    /**
     * 建造者模式
     */
    public static class Builder {
        private final MetricRegistry registry;
        private Logger logger;
        private Locale locale;
        private Clock clock;
        private TimeZone timeZone;
        private TimeUnit rateUnit;
        private TimeUnit durationUnit;
        private MetricFilter filter;

        private Builder(MetricRegistry registry,String metricLogName) {
            this.registry = registry;
            this.logger = LogManager.getLogger(metricLogName);
            this.locale = Locale.getDefault();
            this.clock = Clock.defaultClock();
            this.timeZone = TimeZone.getDefault();
            this.rateUnit = TimeUnit.SECONDS;
            this.durationUnit = TimeUnit.MILLISECONDS;
            this.filter = MetricFilter.ALL;
        }

        public LoggerReporter.Builder outputTo(Logger logger) {
            this.logger = logger;
            return this;
        }

        public LoggerReporter.Builder formattedFor(Locale locale) {
            this.locale = locale;
            return this;
        }

        public LoggerReporter.Builder withClock(Clock clock) {
            this.clock = clock;
            return this;
        }

        public LoggerReporter.Builder formattedFor(TimeZone timeZone) {
            this.timeZone = timeZone;
            return this;
        }

        public LoggerReporter.Builder convertRatesTo(TimeUnit rateUnit) {
            this.rateUnit = rateUnit;
            return this;
        }

        public LoggerReporter.Builder convertDurationsTo(TimeUnit durationUnit) {
            this.durationUnit = durationUnit;
            return this;
        }

        public LoggerReporter.Builder filter(MetricFilter filter) {
            this.filter = filter;
            return this;
        }

        public LoggerReporter build() {
            return new LoggerReporter(this.registry,this.logger,this.locale,this.clock,this.timeZone,this.rateUnit,this.durationUnit,this.filter);
        }
    }
}
