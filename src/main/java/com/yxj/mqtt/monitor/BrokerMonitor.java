package com.yxj.mqtt.monitor;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class BrokerMonitor {
    public static AtomicInteger publishMessageCount = new AtomicInteger(0);

    public static ConcurrentLinkedQueue throughput = new ConcurrentLinkedQueue();

    public AtomicInteger getPublishMessageCount() {
        return publishMessageCount;
    }

}
