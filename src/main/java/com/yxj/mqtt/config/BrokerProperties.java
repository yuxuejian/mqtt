package com.yxj.mqtt.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@ConfigurationProperties(prefix = "netty.mqtt.broker")
public class BrokerProperties{
private int port;
/**worker线程的数量*/
private int selectorThreadNum;
/**业务处理线程池的数量*/
private int workerThreadNum;
/**具体请求分配的线程池的数量*/
private int publicExecutorNum;

private boolean useEpoll;

private int sendBufSize;

private int recvBufSize;

private int keepAlive;

private String mqttBrokerHome;
}
