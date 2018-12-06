package com.yxj.mqtt.bean;

import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import lombok.Data;

@Data
public class Session {
    private String clientId;

    private Channel channel;

    private boolean cleanSession;

    private MqttPublishMessage willMessage;

    public Session() {}

    public Session(String clientId, Channel channel, boolean cleanSession, MqttPublishMessage willMessage) {
        this.clientId = clientId;
        this.channel = channel;
        this.cleanSession = cleanSession;
        this.willMessage = willMessage;
    }
}
