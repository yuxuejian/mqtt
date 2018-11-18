package com.yxj.mqtt.thread;

import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.MqttMessage;

public class RequestTask implements Runnable{
    private final Runnable runnable;
    private final Channel channel;
    private final MqttMessage mqttMessage;

    public RequestTask(final Runnable runnable, final Channel channel, final MqttMessage mqttMessage) {
        this.runnable = runnable;
        this.channel = channel;
        this.mqttMessage = mqttMessage;
    }

    @Override
    public void run() {
        runnable.run();
    }
}
