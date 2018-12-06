package com.yxj.mqtt.bean;

import lombok.Data;

import java.io.Serializable;

@Data
public class PubRelMessage implements Serializable {
    private static final long serialVersionUID = 1l;

    private String clientId;

    private int messageId;

    public PubRelMessage setClientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public PubRelMessage setMessageId(int messageId) {
        this.messageId = messageId;
        return this;
    }
}
