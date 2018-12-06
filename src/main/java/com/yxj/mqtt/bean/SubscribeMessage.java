package com.yxj.mqtt.bean;

import lombok.Data;

import java.io.Serializable;

@Data
public class SubscribeMessage implements Serializable {
    private static final long serialVersionUID = 1l;

    private String clientId;

    private String topicFilter;

    private int mqttQoS;

    public SubscribeMessage(String clientId, String topicFilter, int mqttQoS) {
        this.clientId = clientId;
        this.topicFilter = topicFilter;
        this.mqttQoS = mqttQoS;
    }

    public SubscribeMessage setClientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public SubscribeMessage setTopicFilter(String topicFilter) {
        this.topicFilter = topicFilter;
        return this;
    }

    public SubscribeMessage setMqttQoS(int mqttQoS) {
        this.mqttQoS = mqttQoS;
        return this;
    }
}
