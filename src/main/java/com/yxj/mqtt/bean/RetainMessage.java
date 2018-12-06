package com.yxj.mqtt.bean;

import lombok.Data;

import java.io.Serializable;

@Data
public class RetainMessage implements Serializable {
    private static final long serialVersionUID = -7548204047370972779L;

    private String topic;

    private byte[] messageBytes;

    private int mqttQoS;

    public RetainMessage setTopic(String topic) {
        this.topic = topic;
        return this;
    }

    public RetainMessage setMessageBytes(byte[] messageBytes) {
        this.messageBytes = messageBytes;
        return this;
    }

    public RetainMessage setMqttQoS( int mqttQoS) {
        this.mqttQoS = mqttQoS;
        return this;
    }
}
