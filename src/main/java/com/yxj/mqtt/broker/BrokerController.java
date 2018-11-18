package com.yxj.mqtt.broker;

import com.yxj.mqtt.config.BrokerProperties;
import com.yxj.mqtt.netty.NettyServer;
import com.yxj.mqtt.netty.service.NettyService;
import com.yxj.mqtt.process.impl.DefaultBrokerProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class BrokerController  {

    private static final Logger LOGGER = LoggerFactory.getLogger(BrokerController.class);

    private BrokerProperties brokerProperties;
    private NettyService nettyService;
    private ExecutorService publicExecutor;

    public BrokerController(BrokerProperties brokerProperties) {
        this.brokerProperties = brokerProperties;
    }

    public void initBroker() {
        nettyService = new NettyServer(brokerProperties);
        this.publicExecutor = Executors.newFixedThreadPool(brokerProperties.getPublicExecutorNum(), new ThreadFactory() {
            private AtomicInteger threadIndex = new AtomicInteger(0);
            private int threadTotal = brokerProperties.getPublicExecutorNum();
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, String.format("publicExecutor_%d_%d", threadTotal, threadIndex.incrementAndGet()));
            }
        });
        this.registerProcess();

    }

    public void startBroker() {
        nettyService.start();
    }

    public void shutdownBroker() {
        nettyService.shutdown();
        publicExecutor.shutdown();
    }

    private void registerProcess() {
        nettyService.registerProcess(new DefaultBrokerProcess(), publicExecutor);
    }
}
