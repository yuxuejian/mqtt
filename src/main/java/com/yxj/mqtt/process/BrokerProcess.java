package com.yxj.mqtt.process;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttMessage;

public interface BrokerProcess {
    void processRequest(ChannelHandlerContext ctx, MqttMessage request);
}
