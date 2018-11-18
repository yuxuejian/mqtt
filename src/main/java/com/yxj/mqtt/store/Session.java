package com.yxj.mqtt.store;

import io.netty.channel.Channel;
import lombok.Data;

@Data
public class Session {
    private String clientId;

    private Channel channel;

    private boolean cleanSession;
}
