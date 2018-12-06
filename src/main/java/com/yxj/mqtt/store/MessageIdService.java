package com.yxj.mqtt.store;

public interface MessageIdService {
    /**
     * 获取报文标识符
     */
    int getNextMessageId();

    /**
     * 释放报文标识符
     */
    void releaseMessageId(int messageId);
}
