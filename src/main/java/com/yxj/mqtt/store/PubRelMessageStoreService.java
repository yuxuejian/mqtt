package com.yxj.mqtt.store;

import com.yxj.mqtt.bean.PubRelMessage;

import java.util.List;

public interface PubRelMessageStoreService {

    /**
     * 存储消息
     */
    void put(String clientId, PubRelMessage pubRelMessage);

    /**
     * 获取消息集合
     */
    List<PubRelMessage> get(String clientId);

    /**
     * 删除消息
     */
    void remove(String clientId, int messageId);

    /**
     * 删除消息
     */
    void removeByClient(String clientId);

}
