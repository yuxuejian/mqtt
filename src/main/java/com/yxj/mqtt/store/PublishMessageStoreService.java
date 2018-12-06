package com.yxj.mqtt.store;

import com.yxj.mqtt.bean.PublishMessage;

import java.util.List;

public interface PublishMessageStoreService {

    /**
     * 存储消息
     */
    void put(String clientId, PublishMessage publishMessage);

    /**
     * 获取消息集合
     */
    List<PublishMessage> get(String clientId);

    /**
     * 删除消息
     */
    void remove(String clientId, int messageId);

    /**
     * 删除消息
     */
    void removeByClient(String clientId);

}
