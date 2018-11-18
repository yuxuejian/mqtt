package com.yxj.mqtt.store;

public interface ISessionStore {
    /**
     * 存储会话
     */
    void put(String clientId, Session session);

    /**
     * 获取会话
     */
    Session get(String clientId);

    /**
     * clientId的会话是否存在
     */
    boolean containsKey(String clientId);

    /**
     * 删除会话
     */
    void remove(String clientId);

}
