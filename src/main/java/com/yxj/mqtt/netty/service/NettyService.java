package com.yxj.mqtt.netty.service;

import com.yxj.mqtt.process.BrokerProcess;

import java.util.concurrent.ExecutorService;

public interface NettyService {

    void start();
    void shutdown();
    void registerProcess(final BrokerProcess process, final ExecutorService executorService);
    void registerProcess(final int code, final BrokerProcess process, final ExecutorService executorService);
}
