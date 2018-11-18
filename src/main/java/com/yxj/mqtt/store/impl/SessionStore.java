package com.yxj.mqtt.store.impl;

import com.yxj.mqtt.store.ISessionStore;
import com.yxj.mqtt.store.Session;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionStore implements ISessionStore {
    private Map<String, Session> sessionCache = new ConcurrentHashMap<String, Session>();
    @Override
    public void put(String clientId, Session session) {
        sessionCache.put(clientId, session);
    }

    @Override
    public Session get(String clientId) {
        return sessionCache.get(clientId);
    }

    @Override
    public boolean containsKey(String clientId) {
        return sessionCache.containsKey(clientId);
    }

    @Override
    public void remove(String clientId) {
        sessionCache.remove(clientId);
    }
}
