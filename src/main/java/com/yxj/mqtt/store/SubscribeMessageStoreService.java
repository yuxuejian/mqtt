package com.yxj.mqtt.store;

import com.yxj.mqtt.bean.SubscribeMessage;

import java.util.List;

public interface SubscribeMessageStoreService {
    /**
     * 存储订阅
     */
    void put(String topicFilter, SubscribeMessage subscribeStore);

    /**
     * 删除订阅
     */
    void remove(String topicFilter, String clientId);

    /**
     * 删除clientId的订阅
     */
    void removeForClient(String clientId);

    /**
     * 获取订阅存储集
     */
    List<SubscribeMessage> search(String topic);
}
