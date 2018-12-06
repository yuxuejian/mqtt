package com.yxj.mqtt.bean;

import lombok.Data;

import java.io.Serializable;
@Data
public class PublishMessage implements Serializable {

    private static final long serialVersionUID = 1l;

    private String clientId;

    private String topic;

    private int mqttQoS;

    private int messageId;

    private byte[] messageBytes;

    public PublishMessage setClientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public PublishMessage setTopic(String topic) {
        this.topic = topic;
        return this;
    }

    public PublishMessage setMqttQoS(int mqttQoS) {
        this.mqttQoS = mqttQoS;
        return this;
    }

    public PublishMessage setMessageId(int messageId) {
        this.messageId = messageId;
        return this;
    }

    public PublishMessage setMessageBytes(byte[] messageBytes) {
        this.messageBytes = messageBytes;
        return this;
    }
}
