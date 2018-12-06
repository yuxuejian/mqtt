package com.yxj.mqtt.store;

import com.yxj.mqtt.bean.RetainMessage;

import java.util.List;

public interface RetainMessageStoreService {
    /**
     * 存储retain标志消息
     */
    void put(String topic, RetainMessage retainMessage);

    /**
     * 获取retain消息
     */
    RetainMessage get(String topic);

    /**
     * 删除retain标志消息
     */
    void remove(String topic);

    /**
     * 判断指定topic的retain消息是否存在
     */
    boolean containsKey(String topic);

    /**
     * 获取retain消息集合
     */
    List<RetainMessage> search(String topicFilter);

}
