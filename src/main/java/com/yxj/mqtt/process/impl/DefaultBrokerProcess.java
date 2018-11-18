package com.yxj.mqtt.process.impl;

import com.yxj.mqtt.process.BrokerProcess;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttMessage;

public class DefaultBrokerProcess implements BrokerProcess {
    @Override
    public void processRequest(ChannelHandlerContext ctx, MqttMessage request) {

    }

}
