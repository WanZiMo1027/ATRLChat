package com.yuntian.chat_app.utils;


import java.util.Date;



public class SnowflakeIdGenerator {

    // ===== 配置位宽 =====
    private static final long SEQUENCE_BITS = 12L;       // 每毫秒序列位
    private static final long WORKER_ID_BITS = 5L;       // 机器ID位
    private static final long DATA_CENTER_ID_BITS = 5L;  // 机房ID位

    // ===== 最大值 =====
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    private static final long MAX_DATA_CENTER_ID = ~(-1L << DATA_CENTER_ID_BITS);

    // ===== 位移量 =====
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATA_CENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATA_CENTER_ID_BITS;

    // ===== 纪元（可根据你的项目固定）=====
    private static final long EPOCH = 1655890020000L; // 2022-06-22 17:27:00

    // ===== 时钟回拨容忍毫秒（小范围回拨等待）=====
    private static final long CLOCK_BACKWARD_TOLERATE_MS = 5L;

    private final long dataCenterId;
    private final long workerId;

    // 状态
    private long lastTimestamp = -1L;
    private long sequence = 0L;

    public SnowflakeIdGenerator(long dataCenterId, long workerId) {
        if (dataCenterId < 0 || dataCenterId > MAX_DATA_CENTER_ID) {
            throw new IllegalArgumentException("dataCenterId must be between 0 and " + MAX_DATA_CENTER_ID);
        }
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException("workerId must be between 0 and " + MAX_WORKER_ID);
        }
        this.dataCenterId = dataCenterId;
        this.workerId = workerId;
    }

    public synchronized long nextId() {
        long ts = currentTime();

        // 时钟回拨处理
        if (ts < lastTimestamp) {
            long diff = lastTimestamp - ts;
            if (diff <= CLOCK_BACKWARD_TOLERATE_MS) {
                // 小回拨：等待到 lastTimestamp
                try {
                    Thread.sleep(diff);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Clock back and sleep interrupted", e);
                }
                ts = currentTime();
                if (ts < lastTimestamp) {
                    // 极端情况下仍未追平，直接提升到 lastTimestamp
                    ts = lastTimestamp;
                }
            } else {
                // 大回拨：直接抛错或启用备用序列方案
                throw new IllegalStateException("Clock moved backwards: " + diff + "ms");
            }
        }

        if (ts == lastTimestamp) {
            // 同毫秒内自增
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                // 序列溢出，阻塞到下一毫秒
                ts = nextMillis(lastTimestamp);
            }
        } else {
            // 新毫秒重置
            sequence = 0L;
        }

        lastTimestamp = ts;

        // 组装
        return ((ts - EPOCH) << TIMESTAMP_SHIFT)
                | (dataCenterId << DATA_CENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    private long nextMillis(long lastTs) {
        long ts = currentTime();
        while (ts <= lastTs) {
            ts = currentTime();
        }
        return ts;
    }

    private long currentTime() {
        return System.currentTimeMillis();
    }

    // 解析时间（从ID还原出生成时间）
    public static Date parseTime(long id) {
        long timestampPart = (id >>> TIMESTAMP_SHIFT); // 无符号右移
        long ts = timestampPart + EPOCH;
        return new Date(ts);
    }

    // 简便工厂（默认 1,1）
    public static SnowflakeIdGenerator defaultInstance() {
        return new SnowflakeIdGenerator(1, 1);
    }

    // 随机短串（基于36进制，非严格唯一，展示友好）
    public static String randomStringBase36(SnowflakeIdGenerator gen) {
        return Long.toString(gen.nextId(), Character.MAX_RADIX);
    }
}